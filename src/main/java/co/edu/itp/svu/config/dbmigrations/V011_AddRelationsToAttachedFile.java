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

@ChangeUnit(id = "v011-add-relations-to-attached-file", order = "011", author = "keliumJU")
public class V011_AddRelationsToAttachedFile {

    private final Logger LOG = LoggerFactory.getLogger(V011_AddRelationsToAttachedFile.class);
    private final MongoTemplate template;

    public V011_AddRelationsToAttachedFile(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void changeSet() {
        MongoCollection<Document> attachedFileCollection = template.getCollection("archivo_adjunto");

        String responseAttachmentField = "response_attachment";
        String pqrsAttachmentField = "pqrs_attachment";

        var updateResponseResult = attachedFileCollection.updateMany(
            Filters.exists(responseAttachmentField, false),
            Updates.set(responseAttachmentField, null)
        );
        LOG.info(
            "Added '{}' field (as null) to {} documents in 'archivo_adjunto' collection.",
            responseAttachmentField,
            updateResponseResult.getModifiedCount()
        );

        var updatePqrsResult = attachedFileCollection.updateMany(
            Filters.exists(pqrsAttachmentField, false),
            Updates.set(pqrsAttachmentField, null)
        );
        LOG.info(
            "Added '{}' field (as null) to {} documents in 'archivo_adjunto' collection.",
            pqrsAttachmentField,
            updatePqrsResult.getModifiedCount()
        );
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        MongoCollection<Document> attachedFileCollection = mongoDatabase.getCollection("archivo_adjunto");

        String responseAttachmentField = "response_attachment";
        String pqrsAttachmentField = "pqrs_attachment";

        var unsetResponseResult = attachedFileCollection.updateMany(
            Filters.exists(responseAttachmentField),
            Updates.unset(responseAttachmentField)
        );
        LOG.info(
            "Rolled back: Unset '{}' field from {} documents in 'archivo_adjunto' collection.",
            responseAttachmentField,
            unsetResponseResult.getModifiedCount()
        );

        var unsetPqrsResult = attachedFileCollection.updateMany(Filters.exists(pqrsAttachmentField), Updates.unset(pqrsAttachmentField));
        LOG.info(
            "Rolled back: Unset '{}' field from {} documents in 'archivo_adjunto' collection.",
            pqrsAttachmentField,
            unsetPqrsResult.getModifiedCount()
        );
    }
}
