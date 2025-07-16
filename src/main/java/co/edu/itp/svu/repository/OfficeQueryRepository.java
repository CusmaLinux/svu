package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.Oficina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OfficeQueryRepository {
    /**
     * Searches for Offices entities where the name matches the query.
     *
     * @param query    the string to search for in name and top office.
     * @param pageable the pagination information.
     * @return a page of matching offices entities.
     */
    Page<Oficina> search(String query, Pageable pageable);
}
