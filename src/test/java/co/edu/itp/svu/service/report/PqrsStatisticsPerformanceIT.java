package co.edu.itp.svu.service.report;

import static co.edu.itp.svu.security.AuthoritiesConstants.PQRSD_REPORTS;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.DBRef;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.bson.BsonDocument;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Manual benchmark. Run explicitly via -Dit.test=PqrsStatisticsPerformanceIT. */
@Testcontainers
class PqrsStatisticsPerformanceIT {

    @Container
    static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:8.0.0").withExposedPorts(27017);

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate FIRST_SEEDED_DAY = LocalDate.of(2024, 1, 1);
    private static final LocalDate START = LocalDate.of(2024, 2, 1);
    private static final LocalDate END = LocalDate.of(2024, 10, 31);

    @Test
    void measuresSyntheticVolumesUsingTheProductionPipeline() throws Exception {
        var command = new AtomicReference<BsonDocument>();
        var settings = MongoClientSettings.builder()
            .applyToClusterSettings(cluster -> cluster.hosts(List.of(new ServerAddress(MONGO.getHost(), MONGO.getMappedPort(27017)))))
            .addCommandListener(
                new CommandListener() {
                    @Override
                    public void commandStarted(CommandStartedEvent event) {
                        if ("aggregate".equals(event.getCommandName())) command.set(event.getCommand().clone());
                    }
                }
            )
            .build();
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken("benchmark", null, List.of(new SimpleGrantedAuthority(PQRSD_REPORTS)))
            );
        try (var client = MongoClients.create(settings)) {
            for (int volume : List.of(10_000, 100_000)) {
                MongoTemplate mongo = new MongoTemplate(client, "statistics_bench_" + volume);
                seed(mongo, volume);
                assertThat(
                    mongo.getCollection("pqrs").listIndexes().map(index -> index.getString("name")).into(new ArrayList<>())
                ).containsExactly("_id_");
                var service = new PqrsStatisticsService(new PqrsStatisticsRepository(mongo), new PqrsAuthorizationService(null, null));
                measure(mongo, service, command, volume, Scope.INSTITUTIONAL, null);
                measure(mongo, service, command, volume, Scope.OFFICE, "office-0");
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static void seed(MongoTemplate mongo, int volume) {
        mongo
            .getCollection("oficina")
            .insertMany(
                IntStream.range(0, 20).mapToObj(i -> new Document("_id", "office-" + i).append("nombre", "Oficina sintética " + i)).toList()
            );
        var cases = mongo.getCollection("pqrs");
        String[] states = { "PENDIENTE", "RESUELTA", "RECHAZADA", "EN PROGRESO", "CERRADA" };
        String[] types = { "REQUEST", "COMPLAINT", "CLAIM", "SUGGESTION", "DEMAND" };
        for (int offset = 0; offset < volume; offset += 1000) {
            cases.insertMany(
                IntStream.range(offset, offset + 1000)
                    .mapToObj(i ->
                        new Document(
                            "fecha_creacion",
                            Date.from(FIRST_SEEDED_DAY.plusDays(i % 400).atTime(12, 0).atZone(BOGOTA).toInstant())
                        )
                            .append("oficinaResponder", new DBRef("oficina", "office-" + ((i / 400 + i) % 20)))
                            .append("estado", states[(i / 20) % states.length])
                            .append("type", types[(i / 7) % types.length])
                            .append("descripcion", "x".repeat(512))
                    )
                    .toList()
            );
        }
    }

    private static void measure(
        MongoTemplate mongo,
        PqrsStatisticsService service,
        AtomicReference<BsonDocument> command,
        int volume,
        Scope scope,
        String officeId
    ) throws Exception {
        var filter = new PqrsStatisticsFilter(START, END, scope, officeId);
        var result = service.calculate(filter);
        assertThat(result.received()).isPositive();
        long[] timings = new long[5];
        for (int i = 0; i < timings.length; i++) {
            long start = System.nanoTime();
            assertThat(service.calculate(filter).received()).isEqualTo(result.received());
            timings[i] = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        }
        Arrays.sort(timings);
        var mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var aggregate = command.get();
        var query = new BsonDocument("aggregate", aggregate.getString("aggregate"))
            .append("pipeline", aggregate.getArray("pipeline"))
            .append("cursor", new BsonDocument())
            .append("maxTimeMS", aggregate.get("maxTimeMS"));
        Document full = explain(mongo, query);
        BsonDocument mainOnly = BsonDocument.parse(query.toJson());
        var match = mainOnly.getArray("pipeline").get(0).asDocument().getDocument("$match");
        match.putAll(match.remove("$or").asArray().get(1).asDocument());
        Document main = explain(mongo, mainOnly);
        System.out.printf(
            "STATISTICS_BENCH volume=%d scope=%s received=%d medianMs=%d jsonBytes=%d%n",
            volume,
            scope,
            result.received(),
            timings[2],
            mapper.writeValueAsBytes(result).length
        );
        printPlan("full", full);
        printPlan("main-only", main);
    }

    private static Document explain(MongoTemplate mongo, BsonDocument query) {
        return mongo.getDb().runCommand(new Document("explain", query).append("verbosity", "executionStats"));
    }

    private static void printPlan(String name, Document explain) {
        var stages = explain.getList("stages", Document.class);
        var cursor = stages.get(0).get("$cursor", Document.class);
        var stats = cursor.get("executionStats", Document.class);
        var group = stages.stream().filter(stage -> stage.containsKey("$group")).findFirst().orElseThrow();
        var lookup = stages.stream().filter(stage -> stage.containsKey("$lookup")).findFirst().orElseThrow();
        System.out.printf(
            "STATISTICS_PLAN %s scan=%s docs=%s afterCursor=%s cursorMs=%s groupRows=%s accumulatorBytes=%s usedDisk=%s lookupIndexes=%s lookupDocs=%s lastStageMsEstimate=%s%n",
            name,
            stats.get("executionStages", Document.class).get("stage"),
            stats.get("totalDocsExamined"),
            stats.get("nReturned"),
            stats.get("executionTimeMillis"),
            group.get("nReturned"),
            group.get("maxAccumulatorMemoryUsageBytes"),
            group.get("usedDisk"),
            lookup.get("indexesUsed"),
            lookup.get("totalDocsExamined"),
            stages.get(stages.size() - 1).get("executionTimeMillisEstimate")
        );
    }
}
