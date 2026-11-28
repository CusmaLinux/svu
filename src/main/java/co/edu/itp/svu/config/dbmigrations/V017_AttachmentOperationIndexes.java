package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

/** Non-destructive indexes required by attachment-operation idempotency and recovery. */
@ChangeUnit(id = "v017-attachment-operation-indexes", order = "017", author = "svu")
public class V017_AttachmentOperationIndexes {

    private final MongoTemplate mongo;

    public V017_AttachmentOperationIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Execution
    public void createIndexes() {
        IndexOperations indexes = mongo.indexOps("attachment_operation");
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

    @RollbackExecution
    public void rollback() {}
}
