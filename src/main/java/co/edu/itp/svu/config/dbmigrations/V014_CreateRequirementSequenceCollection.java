package co.edu.itp.svu.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "v014-create-requirement-sequence-index", order = "014", author = "keliumJU")
public class V014_CreateRequirementSequenceCollection {

    private final Logger LOG = LoggerFactory.getLogger(V014_CreateRequirementSequenceCollection.class);

    private final MongoTemplate mongoTemplate;

    private static final String REQUIREMENT_SEQUENCE_COLLECTION_NAME = "requirement_sequence";

    public V014_CreateRequirementSequenceCollection(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void createRequirementSequenceCollection() {
        LOG.info("Executing migration: v014-create-requirement-sequence");
        try {
            if (!mongoTemplate.collectionExists(REQUIREMENT_SEQUENCE_COLLECTION_NAME)) {
                mongoTemplate.createCollection(REQUIREMENT_SEQUENCE_COLLECTION_NAME);
                LOG.info("Collection '{}' created.", REQUIREMENT_SEQUENCE_COLLECTION_NAME);
            } else {
                LOG.info("Collection '{}' already exists. No action taken.", REQUIREMENT_SEQUENCE_COLLECTION_NAME);
            }
        } catch (Exception e) {
            LOG.error("Error during creation of collection '{}': {}", REQUIREMENT_SEQUENCE_COLLECTION_NAME, e.getMessage(), e);
            throw new RuntimeException("Failed to ensure existence of collection " + REQUIREMENT_SEQUENCE_COLLECTION_NAME, e);
        }
    }

    @RollbackExecution
    public void rollback() {
        LOG.warn("Executing rollback for migration: v014-create-requirement-sequence");
        try {
            if (mongoTemplate.collectionExists(REQUIREMENT_SEQUENCE_COLLECTION_NAME)) {
                mongoTemplate.dropCollection(REQUIREMENT_SEQUENCE_COLLECTION_NAME);
                LOG.info("Rollback successful: Dropped collection '{}'.", REQUIREMENT_SEQUENCE_COLLECTION_NAME);
            } else {
                LOG.warn("Rollback notice: Collection '{}' did not exist, nothing to drop.", REQUIREMENT_SEQUENCE_COLLECTION_NAME);
            }
        } catch (Exception e) {
            LOG.error("Error during rollback of v014-create-requirement-sequence: {}", e.getMessage(), e);
        }
    }
}
