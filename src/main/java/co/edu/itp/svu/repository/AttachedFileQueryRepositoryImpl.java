package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/** Searches attachment metadata only when every referenced new root is published. */
@Repository
public class AttachedFileQueryRepositoryImpl implements AttachedFileQueryRepository {

    private final MongoTemplate mongoTemplate;

    public AttachedFileQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<ArchivoAdjunto> search(String queryString, Pageable pageable) {
        List<AggregationOperation> base = visibilityStages();
        if (StringUtils.hasText(queryString)) {
            base.add(
                Aggregation.match(
                    new Criteria()
                        .orOperator(
                            Criteria.where("nombre").regex(Pattern.quote(queryString), "i"),
                            Criteria.where("direct_pqrs_joined.file_number").regex(Pattern.quote(queryString), "i"),
                            Criteria.where("response_pqrs_joined.file_number").regex(Pattern.quote(queryString), "i")
                        )
                )
            );
        }

        List<AggregationOperation> countOperations = new ArrayList<>(base);
        countOperations.add(Aggregation.count().as("totalCount"));
        TypedAggregation<ArchivoAdjunto> countAggregation = Aggregation.newAggregation(ArchivoAdjunto.class, countOperations);
        AggregationResults<CountResult> countResult = mongoTemplate.aggregate(countAggregation, "archivo_adjunto", CountResult.class);
        long total = countResult.getUniqueMappedResult() != null ? countResult.getUniqueMappedResult().getTotalCount() : 0;

        List<AggregationOperation> dataOperations = new ArrayList<>(base);
        if (pageable.getSort().isSorted()) dataOperations.add(Aggregation.sort(pageable.getSort()));
        if (pageable.isPaged()) {
            dataOperations.add(Aggregation.skip(pageable.getOffset()));
            dataOperations.add(Aggregation.limit(pageable.getPageSize()));
        }
        TypedAggregation<ArchivoAdjunto> dataAggregation = Aggregation.newAggregation(ArchivoAdjunto.class, dataOperations);
        List<ArchivoAdjunto> results = mongoTemplate.aggregate(dataAggregation, "archivo_adjunto", ArchivoAdjunto.class).getMappedResults();
        return new PageImpl<>(results, pageable, total);
    }

    private List<AggregationOperation> visibilityStages() {
        LookupOperation lookupDirectCase = Aggregation.lookup("pqrs", "pqrs_attachment.$id", "_id", "direct_pqrs_joined");
        LookupOperation lookupResponse = Aggregation.lookup("respuesta", "response_attachment.$id", "_id", "response_joined");
        LookupOperation lookupResponseCase = Aggregation.lookup("pqrs", "response_joined.pqrs.$id", "_id", "response_pqrs_joined");
        return new ArrayList<>(
            List.of(
                lookupDirectCase,
                Aggregation.unwind("direct_pqrs_joined", true),
                lookupResponse,
                Aggregation.unwind("response_joined", true),
                lookupResponseCase,
                Aggregation.unwind("response_pqrs_joined", true),
                Aggregation.match(new Criteria().andOperator(directVisibility(), responseVisibility()))
            )
        );
    }

    private Criteria directVisibility() {
        return new Criteria()
            .orOperator(
                Criteria.where("pqrs_attachment").exists(false),
                Criteria.where("pqrs_attachment").is(null),
                new Criteria().andOperator(Criteria.where("direct_pqrs_joined._id").exists(true), rootVisibility("direct_pqrs_joined"))
            );
    }

    private Criteria responseVisibility() {
        return new Criteria()
            .orOperator(
                Criteria.where("response_attachment").exists(false),
                Criteria.where("response_attachment").is(null),
                new Criteria()
                    .andOperator(
                        Criteria.where("response_joined._id").exists(true),
                        rootVisibility("response_joined"),
                        Criteria.where("response_joined.pqrs.$id").exists(true),
                        Criteria.where("response_pqrs_joined._id").exists(true),
                        rootVisibility("response_pqrs_joined")
                    )
            );
    }

    private Criteria rootVisibility(String alias) {
        return new Criteria()
            .orOperator(Criteria.where(alias + ".publication").exists(false), Criteria.where(alias + ".publication.state").is("COMMITTED"));
    }

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
