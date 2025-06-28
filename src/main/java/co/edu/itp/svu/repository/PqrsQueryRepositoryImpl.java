package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Pqrs;
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
public class PqrsQueryRepositoryImpl implements PqrsQueryRepository {

    private final MongoTemplate mongoTemplate;

    public PqrsQueryRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<Pqrs> search(String queryString, Pageable pageable) {
        Query query = new Query().with(pageable);

        if (StringUtils.hasText(queryString)) {
            Criteria titleCriteria = Criteria.where("titulo").regex(queryString, "i");
            Criteria fileNumberCriteria = Criteria.where("file_number").regex(queryString, "i");

            Criteria finalCriteria = new Criteria().orOperator(titleCriteria, fileNumberCriteria);
            query.addCriteria(finalCriteria);
        }

        List<Pqrs> pqrsList = mongoTemplate.find(query, Pqrs.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Pqrs.class);

        return new PageImpl<>(pqrsList, pageable, count);
    }
}
