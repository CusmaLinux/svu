package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserQueryRepository {
    /**
     * Searches for Users entities where the name matches the query.
     *
     * @param query    the string to search for in login and email.
     * @param pageable the pagination information.
     * @return a page of matching offices entities.
     */
    Page<User> search(String query, Pageable pageable);
}
