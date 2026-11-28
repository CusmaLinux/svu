package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.LogicalFile;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import co.edu.itp.svu.repository.AttachmentOperationRepository;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Lease;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Version;
import co.edu.itp.svu.repository.AttachmentPublicationRepository;
import co.edu.itp.svu.repository.AttachmentPublicationVerifier;
import co.edu.itp.svu.repository.AttachmentWriterCapability;
import co.edu.itp.svu.service.ResponsePublicationGate;
import co.edu.itp.svu.service.errors.ConflictException;
import co.edu.itp.svu.service.storage.AttachmentRequestIdentity.FileInput;
import co.edu.itp.svu.service.storage.AttachmentStorage.PreparedGeneration;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Coordinates the existing durable operation, physical generation and verified root-publication protocol. */
@Service
public class AttachmentOperationCoordinator {

    private static final Duration CLAIM_TTL = Duration.ofMinutes(2);
    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024;
    private static final long MAX_BATCH_BYTES = 50L * 1024 * 1024;
    private static final int MAX_FILES = 20;

    private final AttachmentOperationRepository operations;
    private final AttachmentStorage storage;
    private final AttachmentPublicationVerifier publicationVerifier;
    private final AttachmentRequestIdentity identities;
    private final ResponsePublicationGate responseGate;
    private final MongoTemplate mongo;

    public AttachmentOperationCoordinator(
        AttachmentOperationRepository operations,
        AttachmentStorage storage,
        AttachmentPublicationVerifier publicationVerifier,
        ResponsePublicationGate responseGate,
        MongoTemplate mongo,
        @Value("${svu.attachments.operation-identity-secret:}") String operationIdentitySecret,
        @Value("${svu.attachments.secure-writers-enabled:false}") boolean secureWritersEnabled
    ) {
        this.operations = operations;
        this.storage = storage;
        this.publicationVerifier = publicationVerifier;
        this.responseGate = responseGate;
        this.mongo = mongo;
        if (secureWritersEnabled && operationIdentitySecret.length() < 32) {
            throw new IllegalStateException("A dedicated attachment operation identity secret is required");
        }
        byte[] identityKey = operationIdentitySecret.isEmpty()
            ? java.security.SecureRandom.getSeed(32)
            : operationIdentitySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        this.identities = new AttachmentRequestIdentity(identityKey);
    }

    public Pqrs publishCase(
        Pqrs pqrs,
        List<MultipartFile> incoming,
        String requestKey,
        List<String> actorScope,
        Map<String, String> fields
    ) {
        List<PreparedInput> files = fingerprint(incoming);
        ObjectId caseId = new ObjectId();
        pqrs.setId(caseId.toHexString());
        AttachmentOperation operation = reserve(
            AttachmentOperationType.PUBLIC_CASE_CREATE,
            null,
            caseId,
            null,
            requestKey,
            actorScope,
            fields,
            files
        );
        requireNewReservation(operation, caseId.toHexString());
        process(operation, files, null, null, pqrs, null);
        return pqrs;
    }

    public Respuesta publishResponse(
        Pqrs parent,
        Respuesta response,
        List<MultipartFile> incoming,
        String requestKey,
        List<String> actorScope,
        Map<String, String> fields,
        AttachmentOperationType type,
        String authorizedOfficeId
    ) {
        if (parent == null || parent.getId() == null) throw new AccessDeniedException("Response case unavailable");
        List<PreparedInput> files = fingerprint(incoming);
        ObjectId responseId = new ObjectId();
        response.setId(responseId.toHexString());
        AttachmentOperation operation = reserve(type, storedId(parent.getId()), null, responseId, requestKey, actorScope, fields, files);
        requireNewReservation(operation, responseId.toHexString());
        process(operation, files, parent, authorizedOfficeId, null, response);
        return response;
    }

    private AttachmentOperation reserve(
        AttachmentOperationType type,
        Object caseId,
        Object newCaseId,
        Object responseId,
        String requestKey,
        List<String> scope,
        Map<String, String> fields,
        List<PreparedInput> files
    ) {
        if (requestKey == null || !canonicalUuid(requestKey)) throw new IllegalArgumentException("A UUID idempotency key is required");
        List<FileInput> fingerprints = files
            .stream()
            .map(file -> new FileInput(file.name(), file.mediaType(), file.size(), file.sha256()))
            .toList();
        var identity = identities.derive(scope, type, caseId, requestKey, fields, fingerprints);
        AttachmentOperation candidate = new AttachmentOperation();
        candidate.setId(UUID.randomUUID().toString());
        candidate.setFormatVersion(2);
        candidate.setOperationType(type);
        candidate.setState(AttachmentOperationState.PREPARING);
        candidate.setRevision(0);
        candidate.setActorScopeHash(identity.actorScopeHash());
        candidate.setRequestKeyHash(identity.requestKeyHash());
        candidate.setRequestDigest(identity.requestDigest());
        candidate.setCaseId(type == AttachmentOperationType.PUBLIC_CASE_CREATE ? newCaseId : caseId);
        candidate.setResponseId(responseId == null ? null : responseId);
        candidate.setPublicationId(UUID.randomUUID().toString());
        candidate.setFiles(new ArrayList<>());
        candidate.setLogicalFiles(
            files.stream().map(file -> new LogicalFile(UUID.randomUUID().toString(), new ObjectId(), file.size(), file.sha256())).toList()
        );
        try {
            return operations.reserve(candidate);
        } catch (AttachmentOperationRepository.IdempotencyConflict | DuplicateKeyException conflict) {
            throw new ConflictException("An operation with this idempotency key already exists");
        }
    }

    private void process(
        AttachmentOperation operation,
        List<PreparedInput> inputs,
        Pqrs parent,
        String authorizedOfficeId,
        Pqrs newCase,
        Respuesta response
    ) {
        // Existing keys only identify the original result. They never return a citizen token or create another root.
        if (!AttachmentOperationState.PREPARING.equals(operation.getState()) || operation.getRevision() != 0) {
            throw new ConflictException("This operation key has already been used");
        }
        UUID owner = UUID.randomUUID();
        Lease lease = operations
            .acquire(version(operation), owner, CLAIM_TTL)
            .map(AttachmentOperationCoordinator::lease)
            .orElseThrow(() -> new ConflictException("The operation is already being processed"));
        boolean gateHeld = false;
        boolean persisting = false;
        try {
            if (parent != null && authorizedOfficeId != null) {
                gateHeld = responseGate.acquire(parent, operation.getId(), authorizedOfficeId);
                if (!gateHeld) {
                    operations.abortBeforePersistence(lease);
                    throw new ConflictException("The case assignment changed; reload the case before responding");
                }
            }

            AttachmentWriterCapability writer = new AttachmentWriterCapability(UUID.fromString(operation.getId()), lease.fencing());
            AttachmentOperation reserved = operations
                .reservePhysicalGeneration(lease, writer)
                .orElseThrow(() -> new ConflictException("The attachment reservation is unavailable"));
            var context = operations
                .authorizePhysicalWrite(lease(reserved), writer)
                .orElseThrow(() -> new ConflictException("The attachment writer lost its reservation"));
            PreparedGeneration prepared;
            Map<String, InputStream> streams = openStreams(inputs, operation.getLogicalFiles());
            try {
                prepared = storage.prepare(context, streams);
            } finally {
                closeStreams(streams);
            }
            var selection = operations.selectPhysicalGeneration(prepared, writer);
            if (
                selection != AttachmentOperationRepository.PhysicalSelection.SELECTED &&
                selection != AttachmentOperationRepository.PhysicalSelection.ALREADY_SELECTED
            ) throw new ConflictException("The verified attachment generation could not be selected");

            AttachmentOperation selected = operations
                .findById(operation.getId())
                .orElseThrow(() -> new ConflictException("Operation unavailable"));
            AttachmentOperation persistingOperation = operations
                .beginVerifiedPersistence(lease(selected))
                .orElseThrow(() -> new ConflictException("The verified attachment generation is no longer current"));
            lease = lease(persistingOperation);
            persisting = true;
            if (parent != null && authorizedOfficeId != null && !responseGateStillHeld(parent.getId(), operation.getId())) {
                operations.requireReconciliation(lease);
                throw new ConflictException("Case assignment verification was lost");
            }

            insertHiddenObjects(operation, inputs, newCase, parent, response, authorizedOfficeId);
            AttachmentPublicationRepository.Result published = publicationVerifier.publishIfVerified(lease);
            if (
                published != AttachmentPublicationRepository.Result.PUBLISHED &&
                published != AttachmentPublicationRepository.Result.ALREADY_PUBLISHED
            ) {
                if (published != AttachmentPublicationRepository.Result.UNCERTAIN) operations.requireReconciliation(lease);
                throw new ConflictException("Publication could not be confirmed; the operation is retained for recovery");
            }
            confirmManifest(operation, lease);
        } catch (RuntimeException | IOException failure) {
            if (persisting) {
                var current = operations.findCurrent(lease);
                if (current.isPresent()) operations.requireReconciliation(lease(current.orElseThrow()));
            } else {
                operations.abortBeforePersistence(lease);
                if (gateHeld && parent != null) responseGate.release(parent.getId(), operation.getId());
            }
            if (failure instanceof RuntimeException runtime) throw runtime;
            throw new IllegalArgumentException("Attachment input could not be read", failure);
        } finally {
            if (gateHeld && parent != null && isRootCommitted(operation)) responseGate.release(parent.getId(), operation.getId());
        }
    }

    private void insertHiddenObjects(
        AttachmentOperation operation,
        List<PreparedInput> inputs,
        Pqrs newCase,
        Pqrs parent,
        Respuesta response,
        String authorizedOfficeId
    ) {
        List<AttachmentOperation.LogicalFile> logicalFiles = operation.getLogicalFiles();
        List<AttachmentOperation.PhysicalEntry> physicalFiles = operations
            .findPhysicalGeneration(operation.getId(), operation.getSelectedPhysicalGeneration())
            .orElseThrow(() -> new ConflictException("Selected generation unavailable"))
            .entries();
        List<ArchivoAdjunto> metadata = new ArrayList<>();
        for (int i = 0; i < logicalFiles.size(); i++) {
            var logical = logicalFiles.get(i);
            var physical = physicalFiles.get(i);
            PreparedInput input = inputs.get(i);
            ArchivoAdjunto file = new ArchivoAdjunto();
            file.setId(logical.metadataId().toHexString());
            file.setNombre(input.name());
            file.setTipo(input.mediaType());
            file.setUrlArchivo(physical.storageKey());
            file.setFechaSubida(Instant.now());
            if (newCase != null) file.setPqrsAttachment(newCase);
            else {
                file.setResponseAttachment(response);
                file.setPqrsAttachment(parent);
            }
            metadata.add(file);
        }
        if (!metadata.isEmpty()) mongo.insertAll(metadata);

        if (newCase != null) {
            newCase.setArchivosAdjuntos(new java.util.HashSet<>(metadata));
            newCase.setPublication(publication(operation));
            mongo.insert(newCase);
        } else {
            response.setPqrs(parent);
            response.setPublication(publication(operation));
            if (authorizedOfficeId != null) {
                response.getPublication().setAuthorizedOfficeId(authorizedOfficeId);
                response.getPublication().setAuthorizationRevision(parent.getAuthorizationRevision());
            }
            response.set_transientAttachments(new java.util.HashSet<>(metadata));
            mongo.insert(response);
        }
    }

    private void confirmManifest(AttachmentOperation operation, Lease lease) {
        String collection = operation.getResponseId() == null ? "pqrs" : "respuesta";
        Object id = operation.getResponseId() == null ? operation.getCaseId() : operation.getResponseId();
        Document root = mongo.getCollection(collection).find(new Document("_id", id)).first();
        Document marker = root == null ? null : root.get("publication", Document.class);
        if (marker == null || !"COMMITTED".equals(marker.getString("state"))) return;
        operations.confirmPublished(
            lease,
            new AttachmentOperationRepository.PublishedRoot(
                collection,
                id,
                operation.getId(),
                operation.getPublicationId(),
                marker.getString("digest"),
                number(marker.get("revision")),
                number(marker.get("manifest_revision")),
                number(marker.get("fencing_token")),
                number(marker.get("selected_generation"))
            )
        );
    }

    private static void requireNewReservation(AttachmentOperation operation, String expectedRootId) {
        String reservedRootId = operation.getResponseId() == null ? operation.getCaseId().toString() : operation.getResponseId().toString();
        if (
            !expectedRootId.equals(reservedRootId) ||
            operation.getState() != AttachmentOperationState.PREPARING ||
            operation.getRevision() != 0 ||
            operation.getClaim() != null
        ) throw new ConflictException("This operation key has already been used");
    }

    private boolean responseGateStillHeld(String caseId, String operationId) {
        return (
            mongo
                .getCollection("pqrs")
                .find(new Document("_id", storedId(caseId)).append("response_publication_lock.operation_id", operationId))
                .first() !=
            null
        );
    }

    private boolean isRootCommitted(AttachmentOperation operation) {
        String collection = operation.getResponseId() == null ? "pqrs" : "respuesta";
        Object id = operation.getResponseId() == null ? operation.getCaseId() : operation.getResponseId();
        Document root = mongo.getCollection(collection).find(new Document("_id", id)).first();
        Document marker = root == null ? null : root.get("publication", Document.class);
        return marker != null && "COMMITTED".equals(marker.getString("state"));
    }

    private Pqrs.Publication publication(AttachmentOperation operation) {
        Pqrs.Publication marker = new Pqrs.Publication();
        marker.setState("PREPARING");
        marker.setOperationId(operation.getId());
        marker.setPublicationId(operation.getPublicationId());
        marker.setDigest(operation.getRequestDigest());
        marker.setRevision(1L);
        return marker;
    }

    private List<PreparedInput> fingerprint(List<MultipartFile> incoming) {
        if (incoming == null || incoming.isEmpty()) return List.of();
        validatePartCount(incoming.size());
        List<PreparedInput> prepared = new ArrayList<>();
        long total = 0;
        for (MultipartFile file : incoming) {
            if (file == null) throw new IllegalArgumentException("Invalid attachment part");
            String name = safeName(file.getOriginalFilename());
            String mediaType = safeMediaType();
            try (InputStream stream = file.getInputStream()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                long length = 0;
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    length += read;
                    total += read;
                    if (length > MAX_FILE_BYTES || total > MAX_BATCH_BYTES) throw new IllegalArgumentException(
                        "Attachment size limit exceeded"
                    );
                    digest.update(buffer, 0, read);
                }
                if (length != file.getSize()) throw new IllegalArgumentException("Attachment size validation failed");
                prepared.add(new PreparedInput(file, name, mediaType, length, HexFormat.of().formatHex(digest.digest())));
            } catch (IOException e) {
                throw new IllegalArgumentException("Attachment could not be read", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 unavailable", e);
            }
        }
        return List.copyOf(prepared);
    }

    private static Map<String, InputStream> openStreams(List<PreparedInput> inputs, List<LogicalFile> logicalFiles) throws IOException {
        Map<String, InputStream> streams = new LinkedHashMap<>();
        try {
            for (int i = 0; i < inputs.size(); i++) streams.put(logicalFiles.get(i).logicalId(), inputs.get(i).file().getInputStream());
            return streams;
        } catch (IOException e) {
            for (InputStream stream : streams.values()) try {
                stream.close();
            } catch (IOException ignored) {}
            throw e;
        }
    }

    private static void closeStreams(Map<String, InputStream> streams) {
        for (InputStream stream : streams.values()) try {
            stream.close();
        } catch (IOException ignored) {}
    }

    private static String safeName(String original) {
        if (original == null || original.isBlank()) throw new IllegalArgumentException("Attachment filename is required");
        String normalized = Normalizer.normalize(original.replace('\\', '/'), Normalizer.Form.NFC);
        String name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (
            name.isBlank() ||
            name.length() > 255 ||
            name.equals(".") ||
            name.equals("..") ||
            name.codePoints().anyMatch(Character::isISOControl)
        ) throw new IllegalArgumentException("Invalid attachment filename");
        return name;
    }

    static void validatePartCount(int partCount) {
        if (partCount < 0 || partCount > MAX_FILES) throw new IllegalArgumentException("Attachment count limit exceeded");
    }

    private static String safeMediaType() {
        // Until content inspection is approved and bounded, new uploads remain opaque and never enter parsers/OCR.
        return "application/octet-stream";
    }

    private static boolean canonicalUuid(String value) {
        try {
            return value != null && UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Object storedId(String id) {
        return ObjectId.isValid(id) ? new ObjectId(id) : id;
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : -1;
    }

    private static Version version(AttachmentOperation operation) {
        return new Version(operation.getId(), operation.getState(), operation.getRevision(), operation.getFormatVersion());
    }

    private static Lease lease(AttachmentOperation operation) {
        return new Lease(version(operation), UUID.fromString(operation.getClaim().getOwnerId()), operation.getClaim().getFencingToken());
    }

    private record PreparedInput(MultipartFile file, String name, String mediaType, long size, String sha256) {}
}
