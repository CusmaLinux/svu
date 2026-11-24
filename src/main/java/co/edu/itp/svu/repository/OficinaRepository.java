package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.service.dto.OfficeOption;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Oficina entity.
 */
@Repository
public interface OficinaRepository extends MongoRepository<Oficina, String>, OfficeQueryRepository {
    @Query(value = "{}", fields = "{ '_id': 1, 'nombre': 1 }")
    Page<OfficeOption> findOptions(Pageable pageable);

    Optional<OfficeOption> findOptionById(String id);

    Optional<Oficina> findByResponsable_Id(String userId);

    Optional<Oficina> findByResponsable_Login(String login);

    Oficina findByNombre(String name);
}
