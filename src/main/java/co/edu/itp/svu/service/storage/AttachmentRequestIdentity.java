package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bson.types.ObjectId;

/** v1 canonical encoding. Only the three MACs may be persisted. The caller supplies the dedicated deployment key. */
public final class AttachmentRequestIdentity {

    private final SecretKeySpec key;

    public AttachmentRequestIdentity(byte[] secret) {
        if (secret.length < 32) throw new IllegalArgumentException("A dedicated key of at least 256 bits is required");
        key = new SecretKeySpec(secret, "HmacSHA256");
    }

    public record Identity(String actorScopeHash, String requestKeyHash, String requestDigest) {}

    /** Name and media type participate in identity but are never stored in the manifest. */
    public record FileInput(String name, String mediaType, long length, String sha256) {
        public FileInput {
            if (length < 0 || !sha256.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid file fingerprint");
        }
    }

    /** scope identifies the verified principal/session. Responses also require the persisted, authorized target. */
    public Identity derive(
        List<String> scope,
        AttachmentOperationType type,
        Object targetCaseId,
        String requestKey,
        Map<String, String> fields,
        List<FileInput> files
    ) {
        if (scope.isEmpty() || scope.stream().anyMatch(String::isBlank) || requestKey.isBlank()) throw new IllegalArgumentException(
            "Missing identity"
        );
        Mac actor = mac("scope");
        count(actor, scope.size());
        scope.forEach(value -> frame(actor, value));
        if (type == AttachmentOperationType.PUBLIC_CASE_CREATE) {
            if (targetCaseId != null) throw new IllegalArgumentException("A new case has no existing target");
        } else if (targetCaseId instanceof ObjectId id) {
            frame(actor, "oid");
            frame(actor, id.toHexString());
        } else if (targetCaseId instanceof String id && !id.isBlank()) {
            frame(actor, "string");
            frame(actor, id);
        } else {
            throw new IllegalArgumentException("A response requires an authorized case");
        }
        String actorHash = finish(actor);
        Mac request = mac("key");
        frame(request, actorHash);
        frame(request, type.name());
        frame(request, requestKey);
        Mac payload = mac("payload");
        frame(payload, actorHash);
        frame(payload, type.name());
        count(payload, fields.size());
        new TreeMap<>(fields).forEach((name, value) -> {
            frame(payload, name);
            frame(payload, value);
        });
        count(payload, files.size());
        for (FileInput file : files) {
            frame(payload, file.name());
            frame(payload, file.mediaType());
            frame(payload, Long.toString(file.length()));
            frame(payload, file.sha256());
        }
        return new Identity(actorHash, finish(request), finish(payload));
    }

    private Mac mac(String domain) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            frame(mac, "svu/attachment/v1/" + domain);
            return mac;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    private static void frame(Mac mac, String value) {
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(value)) throw new IllegalArgumentException("Invalid Unicode");
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        count(mac, bytes.length);
        mac.update(bytes);
    }

    private static void count(Mac mac, int count) {
        mac.update(ByteBuffer.allocate(Integer.BYTES).putInt(count).array());
    }

    private static String finish(Mac mac) {
        return HexFormat.of().formatHex(mac.doFinal());
    }
}
