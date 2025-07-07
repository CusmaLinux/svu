package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Oficina;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Oficina entity.
 */
@Repository
public interface OficinaRepository extends MongoRepository<Oficina, String> {
    Optional<Oficina> findByResponsable_Id(String userId);

    Optional<Oficina> findByResponsable_Login(String login);

    Oficina findByNombre(String name);
}
