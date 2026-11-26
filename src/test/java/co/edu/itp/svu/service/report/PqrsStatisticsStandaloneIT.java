package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import com.mongodb.DBRef;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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

    private static final Instant ORIGINAL = Instant.parse("2024-03-01T04:59:59.999Z");
    private final String syntheticKey = PqrsStatisticsReceiptTest.syntheticKey();
    private final PqrsAuthorizationService policy = new PqrsAuthorizationService(null, null);

    private PqrsStatisticsNarrativeService node(PqrsStatisticsRepository repository, Instant now) {
        return new PqrsStatisticsNarrativeService(
            new PqrsStatisticsService(repository, policy),
            policy,
            PqrsStatisticsReceiptTest.ring("test", "test:" + syntheticKey),
            Clock.fixed(now, ZoneOffset.UTC)
        );
    }

    private static void login(String subject) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new JwtAuthenticationToken(
                    PqrsStatisticsReceiptTest.jwt(subject, "synthetic-" + subject, ORIGINAL.plusSeconds(3600)),
                    List.of(new SimpleGrantedAuthority("ROLE_PQRSD_REPORTS"))
                )
            );
    }

    @Test
    void repeatsOriginalWindowAcrossBogotaMidnightWithoutRefreshingStatisticsAfterCheck() {
        login("analyst");
        var repository = spy(new PqrsStatisticsRepository(template));
        var issuer = node(repository, ORIGINAL);
        var checker = node(repository, ORIGINAL.plusSeconds(300));
        var filter = new PqrsStatisticsFilter(START, LocalDate.of(2024, 2, 29), Scope.INSTITUTIONAL, null);
        var preview = issuer.preview(filter);
        var before = preview.statistics();
        assertThat(before.period().effectiveCutoff()).isEqualTo(ORIGINAL);
        assertThat(before.months().get(0).previousCount()).isEqualTo(1L);
        assertThat(before.months().get(2).complete()).isFalse();
        // Exactly at the original exclusive cutoff, still in February in Bogotá.
        insert(ORIGINAL.toString(), "CLAIM", "PENDING", OFFICE_A);
        var checked = checker.check(filter, preview.receipt());
        verify(repository, times(2)).institutional(any());
        assertThat(checked.statistics()).isEqualTo(before);
        assertThat(checked.checkedAt()).isEqualTo(ORIGINAL.plusSeconds(300));
        insert(ORIGINAL.minusMillis(1).toString(), "CLAIM", "PENDING", OFFICE_A);
        assertThat(checked.statistics()).isEqualTo(before);
        assertThatThrownBy(() -> checked.statistics().months().get(0).types().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> checker.check(filter, preview.receipt())).hasMessage("statisticsChanged");
        verify(repository, times(3)).institutional(any());
    }

    @Test
    void keepsTypedOfficeEvenWhenEmptyAndDetectsRenamesAndAmbiguity() {
        login("analyst");
        var node = node(new PqrsStatisticsRepository(template), ORIGINAL);
        var start = LocalDate.of(2022, 1, 1);
        var filter = new PqrsStatisticsFilter(start, start, Scope.OFFICE, A.toHexString());
        var view = node.preview(filter);
        assertThat(view.statistics().received()).isZero();
        assertThat(view.resolvedOffice()).isEqualTo(new PqrsStatisticsRepository.ResolvedOffice("objectId", A.toHexString(), "Oficina"));
        var other = node.preview(new PqrsStatisticsFilter(start, start, Scope.OFFICE, "B"));
        assertThat(other.resolvedOffice()).isEqualTo(new PqrsStatisticsRepository.ResolvedOffice("string", "B", "Oficina"));
        assertThat(node.check(filter, view.receipt()).statistics()).isEqualTo(view.statistics());
        template.getCollection("oficina").updateOne(new Document("_id", A), new Document("$set", new Document("nombre", "Renombrada")));
        assertThatThrownBy(() -> node.check(filter, view.receipt())).hasMessage("statisticsChanged");
        template.getCollection("oficina").insertOne(new Document("_id", A.toHexString()));
        assertThatThrownBy(() -> node.check(filter, view.receipt())).hasMessage("Unknown or ambiguous office");
        assertThatThrownBy(() -> node.preview(new PqrsStatisticsFilter(start, start, Scope.OFFICE, "absent"))).hasMessage(
            "Unknown or ambiguous office"
        );
    }

    @Test
    void detectsIndividualChangesWithStableTotalInClosedPeriod() {
        login("analyst");
        var node = node(new PqrsStatisticsRepository(template), ORIGINAL);
        var filter = new PqrsStatisticsFilter(START, LocalDate.of(2024, 1, 31), Scope.INSTITUTIONAL, null);
        var match = new Document("type", "DEMAND");
        for (var change : List.of(
            new Document("oficinaResponder", OFFICE_B),
            new Document("estado", "CERRADA"),
            new Document("type", "CLAIM")
        )) {
            var view = node.preview(filter);
            template.getCollection("pqrs").updateOne(match, new Document("$set", change));
            assertThatThrownBy(() -> node.check(filter, view.receipt())).hasMessage("statisticsChanged");
            assertThat(node.preview(filter).statistics().received()).isEqualTo(view.statistics().received());
        }
    }

    @Test
    void concurrentChecksAcrossInstancesIsolateCredentialsAndReadOnceEach() throws Exception {
        login("analyst");
        var repository = spy(new PqrsStatisticsRepository(template));
        var filter = new PqrsStatisticsFilter(START, START, Scope.INSTITUTIONAL, null);
        var receipt = node(repository, ORIGINAL).preview(filter).receipt();
        var pool = Executors.newFixedThreadPool(3);
        var barrier = new CyclicBarrier(3);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (String subject : List.of("analyst", "other", "analyst")) results.add(
                pool.submit(() -> {
                    try {
                        login(subject);
                        var checker = node(repository, ORIGINAL.plusSeconds(1));
                        barrier.await(10, TimeUnit.SECONDS);
                        checker.check(filter, receipt);
                        return "checked";
                    } catch (PqrsStatisticsReceiptException error) {
                        return error.getMessage();
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                })
            );
            assertThat(results.get(0).get(15, TimeUnit.SECONDS)).isEqualTo("checked");
            assertThat(results.get(1).get(15, TimeUnit.SECONDS)).isEqualTo("previewNotFound");
            assertThat(results.get(2).get(15, TimeUnit.SECONDS)).isEqualTo("checked");
            verify(repository, times(3)).institutional(any());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void preparesDocumentOnlyFromTheCheckedResultAndPreservesTablesAndGaps() throws Exception {
        login("analyst");
        var repository = spy(new PqrsStatisticsRepository(template));
        var checking = ORIGINAL.plusSeconds(20);
        var from = new PqrsStatisticsFilter(START, LocalDate.of(2024, 2, 29), Scope.INSTITUTIONAL, null);
        var preview = node(repository, ORIGINAL).preview(from);
        var text = PqrsStatisticsNarrativeText.validate(
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                .objectNode()
                .put("requestsAnalysis", "<p>SYNTHETIC-LITERAL</p>\n\nAnálisis")
        );
        var service = new PqrsStatisticsDocumentService(node(repository, checking), Clock.fixed(checking, ZoneOffset.UTC));
        var document = service.prepare(new PqrsStatisticsDocumentService.ExportRequest(preview.receipt(), from, text));
        var sections = document.sections();
        assertThat(sections).extracting(PqrsStatisticsDocument.Section::number).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(sections).extracting(s -> s.narrative().size()).containsExactly(3, 1, 1, 1, 1, 1, 2);
        assertThat(sections.get(1).narrative().get(0).value()).isEqualTo("<p>SYNTHETIC-LITERAL</p>\n\nAnálisis");
        assertThat(sections.get(6).narrative().get(0).displayText()).isEqualTo("Sin texto redactado.");
        var metadata = document.metadata();
        assertThat(metadata.status()).isEqualTo(Status.PRELIMINARY);
        assertThat(metadata.period()).isEqualTo(preview.statistics().period());
        assertThat(metadata.checkedAt()).isEqualTo(checking);
        assertThat(metadata.generatedAt()).isEqualTo(checking);
        assertThat(metadata.warnings())
            .extracting(Notice::code)
            .containsExactlyInAnyOrderElementsOf(preview.statistics().coverage().warnings());
        var tables = sections.stream().flatMap(s -> s.tables().stream()).toList();
        assertThat(tables)
            .extracting(Table::id)
            .containsExactly(
                TableId.SUMMARY,
                TableId.EVOLUTION,
                TableId.TYPOLOGIES,
                TableId.STATES,
                TableId.MONTHLY_TYPES,
                TableId.VARIATIONS,
                TableId.OFFICES,
                TableId.MONTHLY_OFFICES,
                TableId.INDICATORS,
                TableId.METHODOLOGY
            );
        var evolution = tables.get(1);
        assertThat(evolution.totals().cells().get(1)).isEqualTo(new Quantity(preview.statistics().received()));
        assertThat(evolution.rows().get(2).cells().get(0)).isEqualTo(new PqrsStatisticsDocument.Month(YearMonth.of(2024, 2), true));
        var data = preview.statistics().types();
        assertThat(tables.get(2).rows()).hasSize(data.size());
        for (int i = 0; i < data.size(); i++) {
            assertThat(tables.get(2).rows().get(i).cells()).containsSubsequence(
                new Quantity(data.get(i).count()),
                new Quantity(data.get(i).denominator()),
                new Percentage(data.get(i).percent())
            );
        }
        var rows = tables.get(5).rows();
        assertThat(rows.get(1).cells().get(3)).isEqualTo(new Percentage(new java.math.BigDecimal("-100.0")));
        assertThat(rows.get(2).cells().get(3)).isEqualTo(Missing.PARTIAL_MONTH);
        assertThat(sections.get(5).indicators())
            .filteredOn(i -> i.code().equals("IND-14"))
            .extracting(Indicator::availability)
            .containsExactly(Availability.DEFINITION_PENDING);
        var chart = sections.get(1).charts().stream().filter(c -> c.kind() == ChartKind.LINE).findFirst().orElseThrow();
        assertThat(chart.series().get(0).points().get(2).value()).isNull();
        assertThat(PqrsStatisticsChartImage.png(chart)).hasSizeGreaterThan(1000);
        assertThat(tables.get(6).rows().stream().map(r -> (Category) r.cells().get(0)).filter(c -> "ASSIGNED".equals(c.code())))
            .extracting(Category::idType)
            .containsExactlyInAnyOrder("objectId", "string");
        var json = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .writeValueAsString(document);
        assertThat(json).doesNotContain("PRIVATE_SENTINEL", preview.receipt(), "synthetic-analyst", "access_token", "archivosAdjuntos");
        assertThat(document.toString()).doesNotContain("SYNTHETIC-LITERAL");
        verify(repository, times(2)).institutional(any());
    }

    @Test
    void forwardsInvalidOrExpiredReceiptsBeforeAggregationAndKeepsEmptyOfficeIdentity() {
        login("analyst");
        var repository = spy(new PqrsStatisticsRepository(template));
        var office = new PqrsStatisticsFilter(START.plusYears(-2), START.plusYears(-2), Scope.OFFICE, A.toHexString());
        var preview = node(repository, ORIGINAL).preview(office);
        var empty = PqrsStatisticsNarrativeText.validate(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
        var service = new PqrsStatisticsDocumentService(node(repository, ORIGINAL), Clock.fixed(ORIGINAL, ZoneOffset.UTC));
        var ready = service.prepare(new PqrsStatisticsDocumentService.ExportRequest(preview.receipt(), office, empty));
        assertThat(ready.metadata().office()).isEqualTo(preview.resolvedOffice());
        assertThat(ready.sections().get(0).tables().get(0).rows().get(0).cells().get(1)).isEqualTo(new Quantity(0));
        assertThat(ready.sections().get(4).tables().get(0).rows()).isEmpty();
        assertThatThrownBy(() -> service.prepare(new PqrsStatisticsDocumentService.ExportRequest("INVALID", office, empty))).hasMessage(
            "previewNotFound"
        );
        var expired = new PqrsStatisticsDocumentService(
            node(repository, preview.expiresAt()),
            Clock.fixed(preview.expiresAt(), ZoneOffset.UTC)
        );
        assertThatThrownBy(() -> expired.prepare(new PqrsStatisticsDocumentService.ExportRequest(preview.receipt(), office, empty))
        ).hasMessage("previewExpired");
        verify(repository, times(2)).officeWithIdentity(any(), eq(A.toHexString()));
        verify(repository, never()).institutional(any());
    }

    @Test
    void preservesAllOfficesInTablesAndBoundsOnlyTheGraphs() {
        login("analyst");
        for (int i = 0; i < 18; i++) {
            String identity = String.format(Locale.ROOT, "synthetic-%02d", i);
            template.getCollection("oficina").insertOne(new Document("_id", identity).append("nombre", "Homónima"));
            insert("2024-01-10T05:00:00Z", "COMPLAINT", "PENDING", new DBRef("oficina", identity));
        }
        var repository = new PqrsStatisticsRepository(template);
        var reporting = node(repository, ORIGINAL);
        var filter = new PqrsStatisticsFilter(START, LocalDate.of(2024, 2, 29), Scope.INSTITUTIONAL, null);
        var preview = reporting.preview(filter);
        var empty = PqrsStatisticsNarrativeText.validate(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode());
        var result = new PqrsStatisticsDocumentService(reporting, Clock.fixed(ORIGINAL, ZoneOffset.UTC)).prepare(
            new PqrsStatisticsDocumentService.ExportRequest(preview.receipt(), filter, empty)
        );
        var offices = result.sections().get(4);
        assertThat(offices.tables().get(0).rows()).hasSize(preview.statistics().currentOffices().size());
        assertThat(offices.tables().get(1).rows()).hasSize(
            preview.statistics().months().stream().mapToInt(m -> m.currentOffices().size()).sum()
        );
        assertThat(offices.charts().get(0).categories()).hasSize(16);
        assertThat(offices.charts().get(1).series()).hasSize(8);
        assertThat(offices.charts()).allSatisfy(chart ->
            assertThat(chart.notes()).anyMatch(note -> note.contains("La tabla conserva todas"))
        );
        assertThat(offices.charts().get(1).series()).anyMatch(series -> series.points().stream().anyMatch(point -> point.value() == null));
    }

    @Test
    void devLoggingDoesNotCaptureRequestDocumentOrNarrativeFailure() {
        login("analyst");
        var repository = new PqrsStatisticsRepository(template);
        var reporting = node(repository, ORIGINAL);
        var filter = new PqrsStatisticsFilter(START, START, Scope.INSTITUTIONAL, null);
        var preview = reporting.preview(filter);
        var narrative = PqrsStatisticsNarrativeText.validate(
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("objective", "SYNTHETIC-NARRATIVE-CENTINEL")
        );
        var service = new PqrsStatisticsDocumentService(reporting, Clock.fixed(ORIGINAL, ZoneOffset.UTC));
        var env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("dev");
        var proxies = new org.springframework.aop.aspectj.annotation.AspectJProxyFactory(service);
        proxies.addAspect(new co.edu.itp.svu.aop.logging.LoggingAspect(env));
        PqrsStatisticsDocumentService proxy = proxies.getProxy();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(PqrsStatisticsDocumentService.class);
        var level = logger.getLevel();
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        logger.addAppender(appender);
        try {
            logger.debug("capture enabled");
            var request = new PqrsStatisticsDocumentService.ExportRequest(preview.receipt(), filter, narrative);
            var document = proxy.prepare(request);
            assertThat(document.toString()).doesNotContain("SYNTHETIC-NARRATIVE-CENTINEL", preview.receipt());
            assertThat(request.toString()).doesNotContain("SYNTHETIC-NARRATIVE-CENTINEL", preview.receipt());
            assertThatThrownBy(() -> proxy.prepare(new PqrsStatisticsDocumentService.ExportRequest("SYNTHETIC-INVALID", filter, narrative)))
                .hasMessage("previewNotFound")
                .hasNoCause();
            assertThat(appender.list)
                .extracting(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
                .containsExactly("capture enabled");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
            appender.stop();
        }
    }
}
