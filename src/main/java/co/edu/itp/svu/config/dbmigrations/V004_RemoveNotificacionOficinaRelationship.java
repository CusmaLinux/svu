package co.edu.itp.svu.config.dbmigrations;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.Oficina;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "v004-remove-notification-oficina-relationship", order = "004", author = "keliumJU")
public class V004_RemoveNotificacionOficinaRelationship {

    private final MongoTemplate template;

    public V004_RemoveNotificacionOficinaRelationship(MongoTemplate template) {
        this.template = template;
    }

    @Execution
    public void removeRelationshipFields() {
        Query notificationQuery = new Query();
        Update notificationUpdate = new Update().unset("destinatarios");
        template.updateMulti(notificationQuery, notificationUpdate, Notificacion.class);

        String inverseFieldNameInOffice = "notificacions";

        Query OfficeQuery = new Query();
        Update OfficeUpdate = new Update().unset(inverseFieldNameInOffice);
        template.updateMulti(OfficeQuery, OfficeUpdate, Oficina.class);
    }

    @RollbackExecution
    public void rollback() {}
}
