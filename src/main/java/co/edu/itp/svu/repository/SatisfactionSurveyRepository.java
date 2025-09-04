package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.SatisfactionSurvey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the SatisfactionSurvey entity.
 */
@Repository
public interface SatisfactionSurveyRepository extends MongoRepository<SatisfactionSurvey, String> {}
