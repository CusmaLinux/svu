package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.repository.projection.CaseReferenceProjection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Pqrs entity.
 */
@Repository
public interface PqrsRepository extends MongoRepository<Pqrs, String>, PqrsQueryRepository {
    @Override
    @Query("{ '_id': ?0, 'publication.state': { '$ne': 'PREPARING' } }")
    Optional<Pqrs> findById(String id);

    @Override
    @Query("{ 'publication.state': { '$ne': 'PREPARING' } }")
    List<Pqrs> findAll();

    @Override
    @Query(value = "{ 'publication.state': { '$ne': 'PREPARING' } }")
    Page<Pqrs> findAll(Pageable pageable);

    List<CaseReferenceProjection> findReferencesByOficinaResponder_Id(String oficinaId);

    @Query("{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': ?0, 'fecha_creacion': { '$lte': ?1 } }")
    List<Pqrs> findByEstadoAndFechaCreacionLessThanEqual(String state, LocalDate date, Pageable pageable);

    @Query(value = "{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': { '$ne': ?0 }, 'fecha_creacion': { '$lte': ?1 } }")
    Page<Pqrs> findAllByEstadoNotAndFechaCreacionLessThanEqual(String state, LocalDate date, Pageable pageable);

    @Query(value = "{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': ?0, 'fecha_creacion': { '$lte': ?1 } }")
    Page<Pqrs> findAllByEstadoAndFechaCreacionLessThanEqual(String state, LocalDate date, Pageable pageable);

    @Query(
        value = "{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': { '$ne': ?0 }, 'oficinaResponder.$id': ?1, 'fecha_creacion': { '$lte': ?2 } }"
    )
    Page<Pqrs> findByEstadoNotAndOficinaResponder_IdAndFechaCreacionLessThanEqual(
        String state,
        String officeId,
        LocalDate date,
        Pageable pageable
    );

    @Query(
        value = "{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': ?0, 'oficinaResponder.$id': ?1, 'fecha_creacion': { '$lte': ?2 } }"
    )
    Page<Pqrs> findByEstadoAndOficinaResponder_IdAndFechaCreacionLessThanEqual(
        String state,
        String officeId,
        LocalDate date,
        Pageable pageable
    );

    @Query(
        "{ 'publication.state': { '$ne': 'PREPARING' }, 'fecha_limite_respuesta': { '$gte': ?0, '$lte': ?1 }, 'estado': { '$nin': ?2 } }"
    )
    List<Pqrs> findAllByFechaLimiteRespuestaBetweenAndEstadoNotIn(Instant now, Instant upcomingDate, List<String> states);

    @Query("{ 'access_token': ?0, 'publication.state': { '$ne': 'PREPARING' } }")
    Optional<Pqrs> findByAccessToken(String accessToken);

    @Query("{ 'file_number': ?0, 'publication.state': { '$ne': 'PREPARING' } }")
    Optional<Pqrs> findByFileNumber(String fileNumber);

    @Query(
        "{ 'publication.state': { '$ne': 'PREPARING' }, 'estado': { '$nin': ?0 }, 'fecha_creacion': { '$lt': ?1 }, 'fecha_limite_respuesta': { '$gt': ?2 } }"
    )
    List<Pqrs> findAllByEstadoNotInAndFechaCreacionBeforeAndFechaLimiteRespuestaAfter(
        List<String> statuses,
        Instant fechaCreacion,
        Instant fechaLimite
    );
}
