package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "v008-add-resolver-user-to-answer ", order = "009", author = "luiscarlosjo157")
public class V009_AddResolverUserToRespuesta {

    private final MongoTemplate mongoTemplate;
    private static final String ANSWER_COLLECTION_NAME = "respuesta";

    public V009_AddResolverUserToRespuesta(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void addResolverUserFieldToRespuestaSchema() {
        IndexOperations indexOps = mongoTemplate.indexOps(ANSWER_COLLECTION_NAME);
    }

    @RollbackExecution
    public void rollback() {
        Query query = new Query();
        Update update = new Update().unset("resolver_user");

        mongoTemplate.updateMulti(query, update, ANSWER_COLLECTION_NAME);
    }
}
