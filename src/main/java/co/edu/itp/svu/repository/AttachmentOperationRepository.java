package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/** Persistence primitives only. No background recovery or publication is started by this repository. */
@Repository
public class AttachmentOperationRepository {

    public static final String COLLECTION = "attachment_operation";

    private final MongoTemplate mongo;

    public AttachmentOperationRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Explicit and idempotent. Call only from an authorized migration or isolated test. */
    public void ensureIndexes() {
        IndexOperations indexes = mongo.indexOps(COLLECTION);
        indexes.ensureIndex(
            new Index()
                .on("actor_scope_hash", Sort.Direction.ASC)
                .on("operation_type", Sort.Direction.ASC)
                .on("request_key_hash", Sort.Direction.ASC)
                .unique()
                .named("uq_attachment_operation_idempotency")
        );
        indexes.ensureIndex(
            new Index().on("state", Sort.Direction.ASC).on("updated_at", Sort.Direction.ASC).named("ix_attachment_operation_pending")
        );
        indexes.ensureIndex(
            new Index()
                .on("claim.lease_until", Sort.Direction.ASC)
                .on("state", Sort.Direction.ASC)
                .named("ix_attachment_operation_claim_expiry")
        );
    }

    public AttachmentOperation insert(AttachmentOperation operation) throws DuplicateKeyException {
        return mongo.insert(operation, COLLECTION);
    }

    public Optional<AttachmentOperation> findById(String id) {
        return Optional.ofNullable(mongo.findById(id, AttachmentOperation.class, COLLECTION));
    }

    public List<AttachmentOperation> findPending(Instant before, int limit) {
        Query query = Query.query(
            Criteria.where("state")
                .in(
                    AttachmentOperationState.PREPARING,
                    AttachmentOperationState.FILES_READY,
                    AttachmentOperationState.PERSISTING,
                    AttachmentOperationState.RECONCILIATION_REQUIRED
                )
                .and("updated_at")
                .lte(before)
        )
            .with(Sort.by(Sort.Direction.ASC, "updated_at"))
            .limit(limit);
        return mongo.find(query, AttachmentOperation.class, COLLECTION);
    }

    public List<AttachmentOperation> findExpiredClaims(Instant now, int limit) {
        Query query = Query.query(
            Criteria.where("claim.lease_until")
                .lte(now)
                .and("state")
                .in(
                    AttachmentOperationState.PREPARING,
                    AttachmentOperationState.FILES_READY,
                    AttachmentOperationState.PERSISTING,
                    AttachmentOperationState.RECONCILIATION_REQUIRED
                )
        )
            .with(Sort.by(Sort.Direction.ASC, "claim.lease_until"))
            .limit(limit);
        return mongo.find(query, AttachmentOperation.class, COLLECTION);
    }

    /** Compare-and-set primitive. A zero count means stale state or revision. */
    public boolean compareAndSetState(
        String id,
        AttachmentOperationState expectedState,
        long expectedRevision,
        AttachmentOperationState newState,
        Instant updatedAt
    ) {
        Query query = Query.query(Criteria.where("_id").is(id).and("state").is(expectedState).and("revision").is(expectedRevision));
        Update update = new Update().set("state", newState).set("updated_at", updatedAt).inc("revision", 1);
        return mongo.updateFirst(query, update, COLLECTION).getModifiedCount() == 1;
    }
}
