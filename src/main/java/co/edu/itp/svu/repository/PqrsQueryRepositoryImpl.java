package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Pqrs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class PqrsQueryRepositoryImpl implements PqrsQueryRepository {

    private final MongoTemplate mongoTemplate;

    public PqrsQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Pqrs> search(String queryString, Optional<String> officeId, Pageable pageable) {
        Query query = new Query().with(pageable);
        List<Criteria> allCriteria = new ArrayList<>();

        // A missing marker is a legacy published record; only new PREPARING roots are hidden.
        allCriteria.add(Criteria.where("publication.state").ne("PREPARING"));

        if (StringUtils.hasText(queryString)) {
            Criteria titleCriteria = Criteria.where("titulo").regex(Pattern.quote(queryString), "i");
            Criteria fileNumberCriteria = Criteria.where("file_number").regex(Pattern.quote(queryString), "i");
            allCriteria.add(new Criteria().orOperator(titleCriteria, fileNumberCriteria));
        }

        if (officeId.isPresent()) {
            String id = officeId.orElseThrow();
            // oficinaResponder is stored as a MongoDB DBRef, not an embedded office document.
            Object storedOfficeId = ObjectId.isValid(id) ? new ObjectId(id) : id;
            allCriteria.add(Criteria.where("oficinaResponder.$id").is(storedOfficeId));
        }

        if (!allCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(allCriteria.toArray(new Criteria[0])));
        }

        List<Pqrs> pqrsList = mongoTemplate.find(query, Pqrs.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Pqrs.class);

        return new PageImpl<>(pqrsList, pageable, count);
    }

    @Override
    public List<Pqrs> findPublishedByOffice(String officeId) {
        Object storedOfficeId = ObjectId.isValid(officeId) ? new ObjectId(officeId) : officeId;
        Query query = new Query(
            new Criteria()
                .andOperator(Criteria.where("oficinaResponder.$id").is(storedOfficeId), Criteria.where("publication.state").ne("PREPARING"))
        );
        query.fields().include("_id").include("file_number");
        return mongoTemplate.find(query, Pqrs.class);
    }

    @Override
    public boolean replaceIfAuthorizationCurrent(Pqrs pqrs, long expectedRevision, boolean officeChanged) {
        org.bson.Document replacement = new org.bson.Document();
        mongoTemplate.getConverter().write(pqrs, replacement);
        replacement.put("authorization_revision", expectedRevision + (officeChanged ? 1 : 0));
        replacement.remove("response_publication_lock");
        Object id = storedId(pqrs.getId());
        org.bson.Document revision = expectedRevision == 0
            ? new org.bson.Document(
                "$or",
                List.of(
                    new org.bson.Document("authorization_revision", 0L),
                    new org.bson.Document("authorization_revision", new org.bson.Document("$exists", false))
                )
            )
            : new org.bson.Document("authorization_revision", expectedRevision);
        org.bson.Document filter = new org.bson.Document(
            "$and",
            List.of(
                new org.bson.Document("_id", id)
                    .append("response_publication_lock", new org.bson.Document("$exists", false))
                    .append("publication.state", new org.bson.Document("$ne", "PREPARING")),
                revision
            )
        );
        return mongoTemplate.getCollection("pqrs").replaceOne(filter, replacement).getMatchedCount() == 1;
    }

    @Override
    public boolean updateSatisfactionSurvey(String id, co.edu.itp.svu.domain.SatisfactionSurvey surveyReference) {
        return (
            mongoTemplate
                .updateFirst(
                    Query.query(Criteria.where("_id").is(storedId(id)).and("response_publication_lock").exists(false)),
                    new Update().set("satisfactionSurvey", surveyReference),
                    Pqrs.class
                )
                .getModifiedCount() ==
            1
        );
    }

    @Override
    public boolean updateDeadline(String id, java.time.Instant deadline) {
        return (
            mongoTemplate
                .updateFirst(
                    Query.query(Criteria.where("_id").is(storedId(id))),
                    new Update().set("fechaLimiteRespuesta", deadline),
                    Pqrs.class
                )
                .getModifiedCount() ==
            1
        );
    }

    private static Object storedId(String id) {
        return org.bson.types.ObjectId.isValid(id) ? new org.bson.types.ObjectId(id) : id;
    }
}
