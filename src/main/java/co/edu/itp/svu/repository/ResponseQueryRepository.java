package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Respuesta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResponseQueryRepository {
    /**
     * Searches for answers entities where the name matches the query.
     *
     * @param query    the string to search for the file number of the PQRS that belongs.
     * @param pageable the pagination information.
     * @return a page of matching answers entities.
     */
    Page<Respuesta> search(String query, String officeId, Pageable pageable);
}
