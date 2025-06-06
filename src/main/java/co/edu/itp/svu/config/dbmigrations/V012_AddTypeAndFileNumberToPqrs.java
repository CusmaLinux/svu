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

@ChangeUnit(id = "v012-add-type-and-file-number-to-pqrs", order = "012", author = "keliumJU")
public class V012_AddTypeAndFileNumberToPqrs {

    private static final Logger LOG = LoggerFactory.getLogger(V012_AddTypeAndFileNumberToPqrs.class);

    private static final String PQRS_COLLECTION_NAME = "pqrs";
    private static final String TYPE_FIELD = "type";
    private static final String FILE_NUMBER_FIELD = "file_number";

    private static final String DEFAULT_TYPE_VALUE = "REQUEST";

    private static final String DEFAULT_FILE_NUMBER_VALUE = "NOT_ASSIGNED";

    private final MongoTemplate template;

    public V012_AddTypeAndFileNumberToPqrs(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void changeSet() {
        LOG.info("Executing migration: Add 'type' and 'fileNumber' to 'pqrs' collection.");
        MongoCollection<Document> pqrsCollection = template.getCollection(PQRS_COLLECTION_NAME);

        var filter = Filters.or(Filters.exists(TYPE_FIELD, false), Filters.exists(FILE_NUMBER_FIELD, false));

        var update = Updates.combine(
            Updates.set(TYPE_FIELD, DEFAULT_TYPE_VALUE),
            Updates.set(FILE_NUMBER_FIELD, DEFAULT_FILE_NUMBER_VALUE)
        );

        var result = pqrsCollection.updateMany(filter, update);

        LOG.info(
            "Set default 'type' and 'fileNumber' for {} documents in the '{}' collection.",
            result.getModifiedCount(),
            PQRS_COLLECTION_NAME
        );
    }

    @RollbackExecution
    public void rollback(MongoDatabase mongoDatabase) {
        LOG.warn("Executing rollback: Removing default 'type' and 'fileNumber' from 'pqrs' collection.");
        MongoCollection<Document> pqrsCollection = mongoDatabase.getCollection(PQRS_COLLECTION_NAME);

        var filter = Filters.and(Filters.eq(TYPE_FIELD, DEFAULT_TYPE_VALUE), Filters.eq(FILE_NUMBER_FIELD, DEFAULT_FILE_NUMBER_VALUE));

        var update = Updates.combine(Updates.unset(TYPE_FIELD), Updates.unset(FILE_NUMBER_FIELD));

        var result = pqrsCollection.updateMany(filter, update);

        LOG.info(
            "Rolled back by unsetting 'type' and 'fileNumber' from {} documents in the '{}' collection.",
            result.getModifiedCount(),
            PQRS_COLLECTION_NAME
        );
    }
}
