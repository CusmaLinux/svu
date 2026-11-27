package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Respuesta;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Respuesta entity.
 */
@Repository
public interface RespuestaRepository extends MongoRepository<Respuesta, String>, ResponseQueryRepository {
    @Override
    @Query("{ '_id': ?0, 'publication.state': { '$ne': 'PREPARING' } }")
    java.util.Optional<Respuesta> findById(String id);

    @Override
    @Query("{ 'publication.state': { '$ne': 'PREPARING' } }")
    List<Respuesta> findAll();

    @Override
    @Query(value = "{ 'publication.state': { '$ne': 'PREPARING' } }")
    org.springframework.data.domain.Page<Respuesta> findAll(org.springframework.data.domain.Pageable pageable);

    List<Respuesta> findByPqrsId(String pqrsId);
}
