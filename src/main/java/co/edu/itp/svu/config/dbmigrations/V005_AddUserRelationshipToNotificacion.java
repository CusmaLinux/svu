package co.edu.itp.svu.config.dbmigrations;

import co.edu.itp.svu.domain.Notificacion;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "v005-add-user-relationship-to-notificacion", order = "005", author = "keliumJU")
public class V005_AddUserRelationshipToNotificacion {

    private final MongoTemplate template;

    public V005_AddUserRelationshipToNotificacion(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void addUserFieldToNotificacionSchema() {}

    @RollbackExecution
    public void rollback() {
        Query query = new Query();
        Update update = new Update().unset("recipient");
        template.updateMulti(query, update, Notificacion.class);
    }
}
