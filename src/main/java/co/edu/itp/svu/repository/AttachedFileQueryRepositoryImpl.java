package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class AttachedFileQueryRepositoryImpl implements AttachedFileQueryRepository {

    private final MongoTemplate mongoTemplate;

    public AttachedFileQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<ArchivoAdjunto> search(String queryString, Pageable pageable) {
        if (!StringUtils.hasText(queryString)) {
            Query query = new Query().with(pageable);
            List<ArchivoAdjunto> archivos = mongoTemplate.find(query, ArchivoAdjunto.class);
            long count = mongoTemplate.count(new Query(), ArchivoAdjunto.class);
            return new PageImpl<>(archivos, pageable, count);
        }

        LookupOperation lookupDirectPqrs = Aggregation.lookup("pqrs", "pqrs_attachment.$id", "_id", "direct_pqrs_joined");
        UnwindOperation unwindDirectPqrs = Aggregation.unwind("direct_pqrs_joined", true);

        LookupOperation lookupResponse = Aggregation.lookup("respuesta", "response_attachment.$id", "_id", "response_joined");
        UnwindOperation unwindResponse = Aggregation.unwind("response_joined", true);

        LookupOperation lookupResponsePqrs = Aggregation.lookup("pqrs", "response_joined.pqrs.$id", "_id", "response_pqrs_joined");
        UnwindOperation unwindResponsePqrs = Aggregation.unwind("response_pqrs_joined", true);

        Criteria nombreCriteria = Criteria.where("nombre").regex(queryString, "i");
        Criteria directPqrsFileNumberCriteria = Criteria.where("direct_pqrs_joined.file_number").regex(queryString, "i");
        Criteria responsePqrsFileNumberCriteria = Criteria.where("response_pqrs_joined.file_number").regex(queryString, "i");

        MatchOperation matchOperation = Aggregation.match(
            new Criteria().orOperator(nombreCriteria, directPqrsFileNumberCriteria, responsePqrsFileNumberCriteria)
        );

        TypedAggregation<ArchivoAdjunto> dataAggregation = Aggregation.newAggregation(
            ArchivoAdjunto.class,
            lookupDirectPqrs,
            unwindDirectPqrs,
            lookupResponse,
            unwindResponse,
            lookupResponsePqrs,
            unwindResponsePqrs,
            matchOperation,
            Aggregation.sort(pageable.getSort()),
            Aggregation.skip(pageable.getOffset()),
            Aggregation.limit(pageable.getPageSize())
        );

        List<ArchivoAdjunto> results = mongoTemplate.aggregate(dataAggregation, "archivo_adjunto", ArchivoAdjunto.class).getMappedResults();

        TypedAggregation<ArchivoAdjunto> countAggregation = Aggregation.newAggregation(
            ArchivoAdjunto.class,
            lookupDirectPqrs,
            unwindDirectPqrs,
            lookupResponse,
            unwindResponse,
            lookupResponsePqrs,
            unwindResponsePqrs,
            matchOperation,
            Aggregation.count().as("totalCount")
        );

        AggregationResults<CountResult> countResult = mongoTemplate.aggregate(countAggregation, "archivo_adjunto", CountResult.class);
        long total = countResult.getUniqueMappedResult() != null ? countResult.getUniqueMappedResult().getTotalCount() : 0;

        return new PageImpl<>(results, pageable, total);
    }

    /**
     * Helper class for mapping the $count result.
     */
    private static class CountResult {

        private long totalCount;

        public long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(long totalCount) {
            this.totalCount = totalCount;
        }
    }
}
