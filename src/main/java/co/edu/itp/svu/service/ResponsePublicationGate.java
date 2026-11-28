package co.edu.itp.svu.service;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import co.edu.itp.svu.domain.Pqrs;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/** Serializes institutional response publication against case reassignment on the same case document. */
@Component
public class ResponsePublicationGate {

    private final MongoTemplate mongo;

    public ResponsePublicationGate(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean acquire(Pqrs pqrs, String operationId, String authorizedOfficeId) {
        if (pqrs == null || pqrs.getId() == null || operationId == null || authorizedOfficeId == null) return false;
        Object caseId = storedId(pqrs.getId());
        Object officeId = storedId(authorizedOfficeId);
        Document expectedRevision = pqrs.getAuthorizationRevision() == 0
            ? new Document(
                "$or",
                List.of(new Document("authorization_revision", 0L), new Document("authorization_revision", new Document("$exists", false)))
            )
            : new Document("authorization_revision", pqrs.getAuthorizationRevision());
        Document filter = new Document("_id", caseId)
            .append("oficinaResponder.$id", officeId)
            .append("response_publication_lock", new Document("$exists", false))
            .append("publication.state", new Document("$ne", "PREPARING"));
        Document revisionFilter = new Document("$and", List.of(filter, expectedRevision));
        Document lock = new Document("operation_id", operationId)
            .append("office_id", authorizedOfficeId)
            .append("authorization_revision", pqrs.getAuthorizationRevision());
        Document updated = mongo
            .getCollection("pqrs")
            .findOneAndUpdate(
                revisionFilter,
                combine(set("response_publication_lock", lock)),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            );
        if (updated == null) return false;
        Pqrs.ResponsePublicationLock active = new Pqrs.ResponsePublicationLock();
        active.setOperationId(operationId);
        active.setOfficeId(authorizedOfficeId);
        active.setAuthorizationRevision(pqrs.getAuthorizationRevision());
        pqrs.setResponsePublicationLock(active);
        return true;
    }

    public void release(String caseId, String operationId) {
        if (caseId == null || operationId == null) return;
        mongo
            .getCollection("pqrs")
            .updateOne(
                and(eq("_id", storedId(caseId)), eq("response_publication_lock.operation_id", operationId)),
                unset("response_publication_lock")
            );
    }

    private static Object storedId(String id) {
        return org.bson.types.ObjectId.isValid(id) ? new org.bson.types.ObjectId(id) : id;
    }
}
