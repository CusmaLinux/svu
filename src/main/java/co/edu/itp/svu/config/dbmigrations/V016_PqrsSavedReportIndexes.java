package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "v016-pqrs-saved-report-indexes", order = "016", author = "svu")
public class V016_PqrsSavedReportIndexes {

    private final MongoTemplate mongo;

    public V016_PqrsSavedReportIndexes(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @Execution
    public void createIndexes() {
        mongo
            .indexOps("pqrs_statistics_saved_report")
            .ensureIndex(
                new Index().on("createdBy", Sort.Direction.ASC).on("createdAt", Sort.Direction.DESC).named("saved_report_owner_created")
            );
        mongo
            .indexOps("pqrs_statistics_report_audit")
            .ensureIndex(
                new Index().on("reportId", Sort.Direction.ASC).on("occurredAt", Sort.Direction.DESC).named("saved_report_audit_timeline")
            );
    }

    @RollbackExecution
    public void rollback() {
        if (mongo.collectionExists("pqrs_statistics_saved_report")) mongo
            .indexOps("pqrs_statistics_saved_report")
            .dropIndex("saved_report_owner_created");
        if (mongo.collectionExists("pqrs_statistics_report_audit")) mongo
            .indexOps("pqrs_statistics_report_audit")
            .dropIndex("saved_report_audit_timeline");
    }
}
