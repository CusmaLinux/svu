package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "v008-add-new-fields-to-pqrs", order = "008", author = "luiscarlosjo157")
public class V008_AddNewFieldsToPqrs {

    private final MongoTemplate mongoTemplate;
    private static final String PQRS_COLLECTION_NAME = "pqrs";

    public V008_AddNewFieldsToPqrs(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void addNewFieldsToPqrsSchemaAndCreateIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps(PQRS_COLLECTION_NAME);

        Index accessTokenIndex = new Index().on("access_token", Sort.Direction.ASC).unique().sparse();
        indexOps.ensureIndex(accessTokenIndex);
    }

    @RollbackExecution
    public void rollback() {
        Query query = new Query();
        Update update = new Update().unset("days_to_reply").unset("creating_user").unset("requester_email").unset("access_token");

        mongoTemplate.updateMulti(query, update, PQRS_COLLECTION_NAME);

        IndexOperations indexOps = mongoTemplate.indexOps(PQRS_COLLECTION_NAME);
        try {
            indexOps.dropIndex("access_token_1");
        } catch (Exception e) {
            System.err.println("Could not drop index access_token_1 on " + PQRS_COLLECTION_NAME + " (may not exist): " + e.getMessage());
        }
    }
}
