package co.edu.itp.svu.config.dbmigrations;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.User;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@ChangeUnit(id = "v013-insert-new-office-to-frontdesk-user", order = "013", author = "keliumJU")
public class V013_InsertNewOfficeToFrontdeskUser {

    private final Logger LOG = LoggerFactory.getLogger(V013_InsertNewOfficeToFrontdeskUser.class);

    private final MongoTemplate mongoTemplate;

    private static final String OFFICE_COLLECTION_NAME = "oficina";
    private static final String USER_COLLECTION_NAME = "jhi_user";

    public V013_InsertNewOfficeToFrontdeskUser(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void insertOfficeToFrontdeskUser() {
        LOG.info("Executing migration: v013-insert-new-office-to-frontdesk-user");

        Query adminUserQuery = new Query(Criteria.where("login").is("frontdesk"));
        User responsibleUser = mongoTemplate.findOne(adminUserQuery, User.class, USER_COLLECTION_NAME);

        if (responsibleUser == null) {
            LOG.error("Migration 'v013-insert-new-office-to-frontdesk-user' failed: User 'frontdesk' not found.");
            throw new RuntimeException("Required 'frontdesk' user not found for Oficina migration.");
        } else {
            LOG.info("Found responsible user: {}", responsibleUser.getLogin());
        }

        Oficina reception = new Oficina();
        reception.setNombre("Ventanilla única");
        reception.setDescripcion("Gestiona las solicitudes anónimas.");
        reception.setNivel("3");
        reception.setOficinaSuperior("Secretaría General");
        reception.setResponsable(responsibleUser);

        try {
            mongoTemplate.insert(reception, OFFICE_COLLECTION_NAME);
            LOG.info("Successfully inserted reception office.");
        } catch (Exception e) {
            LOG.error("Error inserting reception office", e.getMessage(), e);
            throw new RuntimeException("Failed to insert reception office.", e);
        }
    }

    @RollbackExecution
    public void rollback() {
        LOG.warn("Executing rollback for migration: v013-insert-new-office-to-frontdesk-user");
        Query rollbackQuery = new Query(Criteria.where("nombre").in("Ventanilla única"));
        try {
            long deletedCount = mongoTemplate.remove(rollbackQuery, OFFICE_COLLECTION_NAME).getDeletedCount();
            LOG.info("Rollback successful: Deleted {} office with name Ventanilla única.", deletedCount);
        } catch (Exception e) {
            LOG.error("Error during rollback of v013-insert-new-office-to-frontdesk-user: {}", e.getMessage(), e);
        }
    }
}
