package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Respuesta;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Respuesta entity.
 */
@Repository
public interface RespuestaRepository extends MongoRepository<Respuesta, String>, ResponseQueryRepository {
    List<Respuesta> findByPqrsId(String pqrsId);
}
