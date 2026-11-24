package co.edu.itp.svu.domain.attachment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/** Durable metadata for a future attachment operation coordinator. */
@Document("attachment_operation")
@CompoundIndexes(
    {
        @CompoundIndex(
            name = "uq_attachment_operation_idempotency",
            def = "{'actor_scope_hash': 1, 'operation_type': 1, 'request_key_hash': 1}",
            unique = true
        ),
        @CompoundIndex(name = "ix_attachment_operation_pending", def = "{'state': 1, 'updated_at': 1}"),
        @CompoundIndex(name = "ix_attachment_operation_claim_expiry", def = "{'claim.lease_until': 1, 'state': 1}"),
    }
)
public class AttachmentOperation {

    @Id
    private String id;

    @Field("format_version")
    private int formatVersion = 1;

    @Field("operation_type")
    private AttachmentOperationType operationType;

    private AttachmentOperationState state;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    private long revision;

    @Field("actor_scope_hash")
    private String actorScopeHash;

    @Field("request_key_hash")
    private String requestKeyHash;

    @Field("request_digest")
    private String requestDigest;

    @Field("case_id")
    private Object caseId;

    @Field("response_id")
    private Object responseId;

    @Field("publication_id")
    private String publicationId;

    private List<FileEntry> files = new ArrayList<>();

    private Claim claim;

    private Evidence evidence;

    @Field("last_failure")
    private String lastFailure;

    public static class FileEntry {

        @Field("metadata_id")
        private ObjectId metadataId;

        @Field("storage_key")
        private String storageKey;

        @Field("size_bytes")
        private Long sizeBytes;

        @Field("sha256")
        private String sha256;

        private String state;

        public ObjectId getMetadataId() {
            return metadataId;
        }

        public void setMetadataId(ObjectId metadataId) {
            this.metadataId = metadataId;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public void setStorageKey(String storageKey) {
            this.storageKey = storageKey;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }

        public void setSizeBytes(Long sizeBytes) {
            this.sizeBytes = sizeBytes;
        }

        public String getSha256() {
            return sha256;
        }

        public void setSha256(String sha256) {
            this.sha256 = sha256;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static class Claim {

        @Field("owner_id")
        private String ownerId;

        @Field("lease_until")
        private Instant leaseUntil;

        @Field("fencing_token")
        private long fencingToken;

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public Instant getLeaseUntil() {
            return leaseUntil;
        }

        public void setLeaseUntil(Instant leaseUntil) {
            this.leaseUntil = leaseUntil;
        }

        public long getFencingToken() {
            return fencingToken;
        }

        public void setFencingToken(long fencingToken) {
            this.fencingToken = fencingToken;
        }
    }

    public static class Evidence {

        @Field("persisted_case_id")
        private Object persistedCaseId;

        @Field("persisted_response_id")
        private Object persistedResponseId;

        @Field("confirmed_at")
        private Instant confirmedAt;

        @Field("root_digest")
        private String rootDigest;

        @Field("publication_revision")
        private Long publicationRevision;

        public Object getPersistedCaseId() {
            return persistedCaseId;
        }

        public void setPersistedCaseId(Object value) {
            persistedCaseId = value;
        }

        public Object getPersistedResponseId() {
            return persistedResponseId;
        }

        public void setPersistedResponseId(Object value) {
            persistedResponseId = value;
        }

        public Instant getConfirmedAt() {
            return confirmedAt;
        }

        public void setConfirmedAt(Instant value) {
            confirmedAt = value;
        }

        public String getRootDigest() {
            return rootDigest;
        }

        public void setRootDigest(String value) {
            rootDigest = value;
        }

        public Long getPublicationRevision() {
            return publicationRevision;
        }

        public void setPublicationRevision(Long value) {
            publicationRevision = value;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int value) {
        formatVersion = value;
    }

    public AttachmentOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(AttachmentOperationType value) {
        operationType = value;
    }

    public AttachmentOperationState getState() {
        return state;
    }

    public void setState(AttachmentOperationState value) {
        state = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        createdAt = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        updatedAt = value;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(long value) {
        revision = value;
    }

    public String getActorScopeHash() {
        return actorScopeHash;
    }

    public void setActorScopeHash(String value) {
        actorScopeHash = value;
    }

    public String getRequestKeyHash() {
        return requestKeyHash;
    }

    public void setRequestKeyHash(String value) {
        requestKeyHash = value;
    }

    public String getRequestDigest() {
        return requestDigest;
    }

    public void setRequestDigest(String value) {
        requestDigest = value;
    }

    public Object getCaseId() {
        return caseId;
    }

    public void setCaseId(Object value) {
        caseId = value;
    }

    public Object getResponseId() {
        return responseId;
    }

    public void setResponseId(Object value) {
        responseId = value;
    }

    public String getPublicationId() {
        return publicationId;
    }

    public void setPublicationId(String value) {
        publicationId = value;
    }

    public List<FileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<FileEntry> value) {
        files = value;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim value) {
        claim = value;
    }

    public Evidence getEvidence() {
        return evidence;
    }

    public void setEvidence(Evidence value) {
        evidence = value;
    }

    public String getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(String value) {
        lastFailure = value;
    }
}
