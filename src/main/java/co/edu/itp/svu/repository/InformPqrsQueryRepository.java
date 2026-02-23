package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.dto.InformPqrsCounts;
import java.time.Instant;
import java.util.Optional;

/**
 * Custom repository interface for InformePqrs entity.
 */
public interface InformPqrsQueryRepository {
    /**
     * Calculates the totals of PQRS (total, resolved, pending) based on a date
     * range and an optional office ID
     * using a single, efficient MongoDB aggregation pipeline.
     *
     * @param fechaInicio the start date of the period (inclusive).
     * @param fechaFin    the end date of the period (inclusive).
     * @param oficinaId   the ID of the office to filter by (optional, can be null).
     * @return an Optional containing the calculated counts, or an empty Optional if
     *         no matching PQRS are found.
     */
    Optional<InformPqrsCounts> calculatePqrsTotals(Instant fechaInicio, Instant fechaFin, String oficinaId);
}
