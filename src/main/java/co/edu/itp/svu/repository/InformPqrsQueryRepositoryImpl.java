package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.dto.InformPqrsCounts;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;

/**
 * Implementation of the custom repository methods for InformePqrs.
 */
public class InformPqrsQueryRepositoryImpl implements InformPqrsQueryRepository {

    private final MongoTemplate mongoTemplate;

    public InformPqrsQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<InformPqrsCounts> calculatePqrsTotals(Instant fechaInicio, Instant fechaFin, String oficinaId) {
        List<Criteria> matchCriteria = new ArrayList<>();
        matchCriteria.add(Criteria.where("fecha_creacion").gte(fechaInicio).lte(fechaFin));

        if (oficinaId != null && !oficinaId.isBlank()) {
            matchCriteria.add(Criteria.where("oficinaResponder.$id").is(new ObjectId(oficinaId)));
        }

        MatchOperation matchStage = Aggregation.match(new Criteria().andOperator(matchCriteria.toArray(new Criteria[0])));

        GroupOperation groupStage = Aggregation.group()
            .count()
            .as("totalPqrs")
            .sum(ConditionalOperators.when(Criteria.where("estado").is(PqrsStatus.RESOLVED.getDisplayName())).then(1).otherwise(0))
            .as("totalResueltas")
            .sum(ConditionalOperators.when(Criteria.where("estado").is(PqrsStatus.PENDING.getDisplayName())).then(1).otherwise(0))
            .as("totalPendientes");

        Aggregation aggregation = Aggregation.newAggregation(matchStage, groupStage);
        AggregationResults<InformPqrsCounts> results = mongoTemplate.aggregate(aggregation, "pqrs", InformPqrsCounts.class);

        return Optional.ofNullable(results.getUniqueMappedResult());
    }
}
