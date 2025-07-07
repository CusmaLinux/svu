package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Pqrs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PqrsQueryRepository {
    /**
     * Searches for Pqrs entities where the title OR fileNumber matches the query.
     *
     * @param query    the string to search for in title and fileNumber.
     * @param pageable the pagination information.
     * @return a page of matching Pqrs entities.
     */
    Page<Pqrs> search(String query, String OfficeId, Pageable pageable);
}
