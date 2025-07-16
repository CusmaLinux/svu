package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Oficina;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class OfficeQueryRepositoryImpl implements OfficeQueryRepository {

    private final MongoTemplate mongoTemplate;

    public OfficeQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Oficina> search(String queryString, Pageable pageable) {
        Query query = new Query().with(pageable);

        if (StringUtils.hasText(queryString)) {
            List<Criteria> orExpressionCriteria = new ArrayList<>();
            orExpressionCriteria.add(Criteria.where("nombre").regex(queryString, "i"));
            orExpressionCriteria.add(Criteria.where("oficina_superior").regex(queryString, "i"));

            Criteria orCriteria = new Criteria().orOperator(orExpressionCriteria.toArray(new Criteria[0]));

            query.addCriteria(orCriteria);
        }

        List<Oficina> oficinas = mongoTemplate.find(query, Oficina.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Oficina.class);

        return new PageImpl<>(oficinas, pageable, count);
    }
}
