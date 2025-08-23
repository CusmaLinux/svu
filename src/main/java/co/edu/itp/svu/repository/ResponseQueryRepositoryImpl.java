package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Respuesta;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ResponseQueryRepositoryImpl implements ResponseQueryRepository {

    private final MongoTemplate mongoTemplate;

    public ResponseQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Respuesta> search(String querySearch, String officeId, Pageable pageable) {
        List<AggregationOperation> aggregationOperations = new ArrayList<>();

        aggregationOperations.add(Aggregation.lookup("pqrs", "pqrs.$id", "_id", "pqrsDetails"));

        aggregationOperations.add(Aggregation.unwind("pqrsDetails"));

        List<Criteria> allCriteria = new ArrayList<>();

        if (StringUtils.hasText(officeId)) {
            allCriteria.add(Criteria.where("pqrsDetails.oficinaResponder.$id").is(new ObjectId(officeId)));
        }

        if (StringUtils.hasText(querySearch)) {
            allCriteria.add(Criteria.where("pqrsDetails.file_number").regex(querySearch, "i"));
        }

        if (!allCriteria.isEmpty()) {
            Criteria matchCriteria = new Criteria().andOperator(allCriteria.toArray(new Criteria[0]));
            aggregationOperations.add(Aggregation.match(matchCriteria));
        }

        List<AggregationOperation> countOperations = new ArrayList<>(aggregationOperations);
        countOperations.add(Aggregation.count().as("total"));
        TypedAggregation<CountResult> countAggregation = Aggregation.newAggregation(CountResult.class, countOperations);
        AggregationResults<CountResult> countResults = mongoTemplate.aggregate(countAggregation, "respuesta", CountResult.class);
        long total = countResults.getUniqueMappedResult() != null ? countResults.getUniqueMappedResult().getTotal() : 0;

        if (pageable.getSort().isSorted()) {
            aggregationOperations.add(Aggregation.sort(remapSort(pageable.getSort())));
        }

        aggregationOperations.add(Aggregation.skip(pageable.getOffset()));

        aggregationOperations.add(Aggregation.limit(pageable.getPageSize()));

        TypedAggregation<Respuesta> dataAggregation = Aggregation.newAggregation(Respuesta.class, aggregationOperations);
        List<Respuesta> respuestas = mongoTemplate.aggregate(dataAggregation, "respuesta", Respuesta.class).getMappedResults();

        return new PageImpl<>(respuestas, pageable, total);
    }

    /**
     * Remaps sort fields to match the structure after the $lookup aggregation.
     * For example, a request to sort by "file_number" becomes "pqrsDetails.file_number".
     */
    private Sort remapSort(Sort originalSort) {
        List<Sort.Order> remappedOrders = originalSort
            .stream()
            .map(order -> {
                String property = order.getProperty();
                if (property.equals("file_number") || property.equals("fecha_creacion") || property.equals("estado")) {
                    return order.withProperty("pqrsDetails." + property);
                }
                return order;
            })
            .collect(Collectors.toList());
        return Sort.by(remappedOrders);
    }

    // Helper class to map the result of the $count stage
    private static class CountResult {

        private long total;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }
}
