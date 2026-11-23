package co.edu.itp.svu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Integration coverage for office DBRef filtering in PQRS searches. */
@IntegrationTest
class PqrsOfficeQueryIT {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PqrsQueryRepositoryImpl queryRepository;

    @Test
    void searchOnlyReturnsCasesAssignedToRequestedOffice() {
        String marker = "office-filter-" + UUID.randomUUID();
        Oficina first = new Oficina();
        first.setId(UUID.randomUUID().toString());
        first.setNombre(marker + "-first");
        Oficina second = new Oficina();
        second.setId(UUID.randomUUID().toString());
        second.setNombre(marker + "-second");

        Pqrs firstCase = new Pqrs().type(PqrsType.REQUEST).titulo(marker).estado("PENDIENTE").oficinaResponder(first);
        Pqrs secondCase = new Pqrs().type(PqrsType.REQUEST).titulo(marker).estado("PENDIENTE").oficinaResponder(second);

        try {
            mongoTemplate.save(first);
            mongoTemplate.save(second);
            mongoTemplate.save(firstCase);
            mongoTemplate.save(secondCase);

            var results = queryRepository.search(marker, first.getId(), PageRequest.of(0, 20));
            assertThat(results.getContent()).extracting(Pqrs::getId).containsExactly(firstCase.getId());
        } finally {
            if (firstCase.getId() != null) mongoTemplate.remove(firstCase);
            if (secondCase.getId() != null) mongoTemplate.remove(secondCase);
            mongoTemplate.remove(first);
            mongoTemplate.remove(second);
        }
    }
    @Test
    void searchTreatsRegexMetacharactersAsLiteralText() {
        String marker = "literal-" + UUID.randomUUID();
        Pqrs matching = new Pqrs().type(PqrsType.REQUEST).titulo(marker + "[test]").estado("PENDIENTE");
        Pqrs other = new Pqrs().type(PqrsType.REQUEST).titulo(marker + "test").estado("PENDIENTE");
        try {
            mongoTemplate.save(matching);
            mongoTemplate.save(other);
            var results = queryRepository.search(marker + "[test]", null, PageRequest.of(0, 20));
            assertThat(results.getContent()).extracting(Pqrs::getId).containsExactly(matching.getId());
        } finally {
            if (matching.getId() != null) mongoTemplate.remove(matching);
            if (other.getId() != null) mongoTemplate.remove(other);
        }
    }

}
