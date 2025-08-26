package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.NonBusinessDay;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NonBusinessDayRepository extends MongoRepository<NonBusinessDay, String> {
    Set<NonBusinessDay> findAllByDateAfter(LocalDate date);
}
