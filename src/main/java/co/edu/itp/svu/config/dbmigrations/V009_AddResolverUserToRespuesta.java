package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "v009-add-resolver-user-to-answer ", order = "009", author = "luiscarlosjo157")
public class V009_AddResolverUserToRespuesta {

    private final Logger LOG = LoggerFactory.getLogger(V007_AddFrontDeskCsAuthority.class);
    private final MongoTemplate mongoTemplate;
    private static final String ANSWER_COLLECTION_NAME = "respuesta";

    public V009_AddResolverUserToRespuesta(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void addResolverUserFieldToRespuestaSchema() {}

    @RollbackExecution
    public void rollback() {
        Query query = new Query();
        Update update = new Update().unset("resolver_user");
        mongoTemplate.updateMulti(query, update, ANSWER_COLLECTION_NAME);
        LOG.info("Rollback of resolve_user param");
    }
}
