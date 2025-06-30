package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.InformePqrs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the InformePqrs entity.
 */
@Repository
public interface InformePqrsRepository extends MongoRepository<InformePqrs, String>, InformPqrsQueryRepository {}
