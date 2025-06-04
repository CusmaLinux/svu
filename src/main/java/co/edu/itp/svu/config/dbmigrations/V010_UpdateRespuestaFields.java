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

@ChangeUnit(id = "v010-update-respuesta-fields", order = "010", author = "keliumJU")
public class V010_UpdateRespuestaFields {

    private final Logger LOG = LoggerFactory.getLogger(V010_UpdateRespuestaFields.class);
    private final MongoTemplate template;

    public V010_UpdateRespuestaFields(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void changeSet() {
        MongoCollection<Document> responseCollection = template.getCollection("respuesta");

        var unsetResult = responseCollection.updateMany(Filters.exists("archivosAdjuntos"), Updates.unset("archivosAdjuntos"));
        LOG.info("Unset 'archivosAdjuntos' field from {} documents in 'respuesta' collection.", unsetResult.getModifiedCount());

        var renameResult = responseCollection.updateMany(Filters.exists("pqr"), Updates.rename("pqr", "pqrs"));
        LOG.info("Renamed 'pqr' field to 'pqrs' in {} documents in 'respuesta' collection.", renameResult.getModifiedCount());
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        MongoCollection<Document> responseCollection = mongoDatabase.getCollection("respuesta");

        var renameRollbackResult = responseCollection.updateMany(Filters.exists("pqrs"), Updates.rename("pqrs", "pqr"));
        LOG.info(
            "Rolled back rename of 'pqrs' to 'pqr' in {} documents in 'respuesta' collection.",
            renameRollbackResult.getModifiedCount()
        );
    }
}
