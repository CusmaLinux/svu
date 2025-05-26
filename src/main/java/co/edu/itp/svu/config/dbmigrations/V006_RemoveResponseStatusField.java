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

@ChangeUnit(id = "remove-estado-from-respuesta-collection", order = "006", author = "luiscarlosjo157")
public class V006_RemoveResponseStatusField {

    private final Logger log = LoggerFactory.getLogger(V006_RemoveResponseStatusField.class);

    @Execution
    public void changeSet(MongoDatabase mongoDatabase) {
        log.info("Executing migration: remove-estado-from-respuesta-collection");
        MongoCollection<Document> respuestaCollection = mongoDatabase.getCollection("respuesta");
        var result = respuestaCollection.updateMany(Filters.exists("estado"), Updates.unset("estado"));
        log.info("Removed 'estado' field from {} documents in 'respuesta' collection.", result.getModifiedCount());
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        log.warn(
            "Rollback for 'remove-estado-from-respuesta-collection': Not automatically reversible. Manual intervention may be needed if 'estado' needs to be restored."
        );
    }
}
