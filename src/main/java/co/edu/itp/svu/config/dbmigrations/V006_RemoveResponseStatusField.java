package co.edu.itp.svu.config.dbmigrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "v006-remove-state-from-answer-collection", order = "006", author = "luiscarlosjo157")
public class V006_RemoveResponseStatusField {

    private final Logger LOG = LoggerFactory.getLogger(V006_RemoveResponseStatusField.class);
    private final MongoTemplate template;

    public V006_RemoveResponseStatusField(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void changeSet() {
        MongoCollection<Document> responseCollection = template.getCollection("respuesta");
        var result = responseCollection.updateMany(Filters.exists("estado"), Updates.unset("estado"));
        LOG.info("Removed 'estado' field from {} documents in 'respuesta' collection.", result.getModifiedCount());
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {}
}
