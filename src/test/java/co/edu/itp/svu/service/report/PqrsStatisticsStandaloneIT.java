package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class PqrsStatisticsStandaloneIT {

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:8.0.0").withExposedPorts(27017);

    static MongoClient client;
    static MongoTemplate template;
    static PqrsStatisticsService service;
    static final ObjectId A = new ObjectId();
    static final DBRef OFFICE_A = new DBRef("oficina", A);
    static final DBRef OFFICE_B = new DBRef("oficina", "B");
    static final LocalDate START = LocalDate.of(2023, 12, 1), END = LocalDate.of(2024, 3, 31);

    @BeforeAll
    static void start() {
        client = MongoClients.create("mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017));
        template = new MongoTemplate(client, "svu_statistics_test");
        Document hello = template.executeCommand("{hello:1}");
        assertThat(hello).doesNotContainKeys("setName", "hosts");
        assertThat(hello.getBoolean("isWritablePrimary")).isTrue();
        service = new PqrsStatisticsService(new PqrsStatisticsRepository(template), new PqrsAuthorizationService(null, null));
    }

    @AfterAll
    static void close() {
        if (client != null) client.close();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void seed() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken("test", null, List.of(new SimpleGrantedAuthority("ROLE_PQRSD_REPORTS")))
            );
        template.dropCollection("pqrs");
        template.dropCollection("oficina");
        template
            .getCollection("oficina")
            .insertMany(List.of(new Document("_id", A).append("nombre", "Oficina"), new Document("_id", "B").append("nombre", "Oficina")));
        insert("2023-12-01T04:59:59.999Z", "REQUEST", "PENDING", OFFICE_A);
        insert("2023-12-01T05:00:00Z", "DEMAND", "RESUELTA", OFFICE_A);
        insert("2024-01-01T04:59:59.999Z", "REQUEST", "EN PROGRESO", OFFICE_B);
        insert("2024-02-01T05:00:00Z", "PRIVATE_SENTINEL", "PRIVATE_SENTINEL", null);
        insert("2024-02-29T18:00:00Z", "CLAIM", "CLOSED", new DBRef("oficina", "missing"));
        insert("2024-03-01T04:59:59.999Z", null, "RECHAZADA", new DBRef("elsewhere", A));
        insert("2024-03-01T05:00:00Z", "SUGGESTION", "PENDIENTE", new DBRef("other_database", "oficina", A));
        insert("2024-04-01T04:59:59.999Z", "COMPLAINT", "PENDING", "old-shape");
        insert("2024-04-01T05:00:00Z", "REQUEST", "PENDING", OFFICE_A);
        template
            .getCollection("pqrs")
            .insertMany(
                List.of(
                    new Document("fecha_creacion", "2024-02-01"),
                    new Document("fecha_creacion", List.of(Date.from(Instant.parse("2024-02-01T05:00:00Z")))),
                    new Document("estado", "PENDIENTE")
                )
            );
    }

    static void insert(String instant, String type, String state, Object office) {
        template
            .getCollection("pqrs")
            .insertOne(
                new Document("fecha_creacion", Date.from(Instant.parse(instant)))
                    .append("type", type)
                    .append("file_number", "NOT_ASSIGNED")
                    .append("estado", state)
                    .append("oficinaResponder", office)
                    .append("requester_email", "PRIVATE_SENTINEL")
                    .append("access_token", "PRIVATE_SENTINEL")
                    .append("archivosAdjuntos", List.of("one", "two"))
            );
    }

    static PqrsStatistics report(Scope scope, String id) {
        return service.calculate(new PqrsStatisticsFilter(START, END, scope, id));
    }

    @Test
    void reassignmentAndStateChangeAreObservedOnTheOriginalReceptionMonth() {
        var match = new Document("type", "DEMAND");
        template
            .getCollection("pqrs")
            .updateOne(
                match,
                new Document(
                    "$set",
                    new Document("oficinaResponder", OFFICE_B)
                        .append("estado", "CERRADA")
                        .append("type", "REQUEST")
                        .append("file_number", "TEST-001")
                )
            );
        assertThat(report(Scope.OFFICE, A.toHexString()).received()).isZero();
        var updated = report(Scope.OFFICE, "B");
        assertThat(updated.months().get(0).received()).isEqualTo(2);
        assertThat(updated.currentStates()).containsEntry("CLOSED", 1L);
        assertThat(updated.types()).filteredOn(t -> t.code().equals("REQUEST")).extracting("count").containsExactly(1L);
    }

    @Test
    void countsRootsAndPartitionsWithoutReturningCaseData() {
        var r = report(Scope.INSTITUTIONAL, null);
        assertThat(r.received()).isEqualTo(7);
        assertThat(r.months()).extracting("received").containsExactly(2L, 0L, 3L, 2L);
        assertThat(r.months().get(0).variationPercent()).isEqualByComparingTo("100");
        assertThat(r.months().get(1).variationPercent()).isEqualByComparingTo("-100");
        assertThat(r.months().get(2).comparison()).isEqualTo("NO_CALCULABLE");
        assertThat(r.months().get(2).variationPercent()).isNull();
        assertThat(r.months().get(3).variationPercent()).isEqualByComparingTo("-33.3");
        assertThat(r.types())
            .filteredOn(t -> t.count() == 1)
            .extracting("code")
            .containsExactlyInAnyOrder("DEMAND", "HISTORICALLY_AMBIGUOUS", "UNKNOWN", "MISSING", "CLAIM", "SUGGESTION", "COMPLAINT");
        assertThat(r.types())
            .filteredOn(t -> t.count() == 1)
            .allMatch(t -> t.denominator() == 7 && t.percent().toPlainString().equals("14.3"));
        assertThat(r.currentStates())
            .containsEntry("UNKNOWN", 1L)
            .containsEntry("RESOLVED", 1L)
            .containsEntry("IN_PROGRESS", 1L)
            .containsEntry("CLOSED", 1L)
            .containsEntry("REJECTED", 1L)
            .containsEntry("PENDING", 2L);
        assertThat(r.currentOffices()).filteredOn(o -> o.category().equals("UNVERIFIABLE")).extracting("count").containsExactly(4L);
        assertThat(r.currentOffices()).filteredOn(o -> o.category().equals("UNASSIGNED")).extracting("count").containsExactly(1L);
        assertThat(r.currentOffices().stream().mapToLong(PqrsStatistics.OfficeCount::count).sum()).isEqualTo(r.received());
        assertThat(r.coverage().warnings()).contains("TYPE_HISTORICALLY_AMBIGUOUS", "OFFICE_UNVERIFIABLE");
        assertThat(r.toString()).doesNotContain("PRIVATE_SENTINEL", "old-shape", "requester_email", "access_token");
    }

    @Test
    void resolvesTypedOfficeAndFiltersComparisonPeriod() {
        var a = report(Scope.OFFICE, A.toHexString());
        assertThat(a.received()).isEqualTo(1);
        assertThat(a.months().get(0).variationPercent()).isEqualByComparingTo("0");
        assertThat(a.currentOffices()).extracting("id").containsExactly(A.toHexString());
        var b = report(Scope.OFFICE, "B");
        assertThat(b.received()).isEqualTo(1);
        assertThat(b.currentOffices()).extracting("idType").containsExactly("string");
        assertThat(b.months().get(0).comparison()).isEqualTo("NO_CALCULABLE");
        assertThatIllegalArgumentException().isThrownBy(() -> report(Scope.OFFICE, "missing"));
        template.getCollection("oficina").insertOne(new Document("_id", A.toHexString()));
        assertThatIllegalArgumentException().isThrownBy(() -> report(Scope.OFFICE, A.toHexString()));
    }
}
