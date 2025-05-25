package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Notificacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Notificacion entity.
 */
@Repository
public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    @Query("{}")
    Page<Notificacion> findAllWithEagerRelationships(Pageable pageable);

    @Query("{}")
    List<Notificacion> findAllWithEagerRelationships();

    @Query("{'id': ?0}")
    Optional<Notificacion> findOneWithEagerRelationships(String id);

    Page<Notificacion> findAllByRecipientId(String recipientId, Pageable pageable);

    Page<Notificacion> findAllByRecipientIdAndLeidoOrderByFechaDesc(String recipientId, Boolean read, Pageable pageable);
}
