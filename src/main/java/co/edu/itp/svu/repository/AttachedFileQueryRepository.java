package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AttachedFileQueryRepository {
    /**
     * Searches for AttachedFile entities where the name matches the query.
     *
     * @param query    the string to search for in name and top attached file.
     * @param pageable the pagination information.
     * @return a page of matching attached file entities.
     */
    Page<ArchivoAdjunto> search(String query, Pageable pageable);
}
