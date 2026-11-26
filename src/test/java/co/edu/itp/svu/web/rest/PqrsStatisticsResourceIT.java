package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import co.edu.itp.svu.service.report.PqrsStatisticsService;
import co.edu.itp.svu.service.report.PqrsStatisticsWorkbook;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBRef;
import com.mongodb.MongoException;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "gemini.api-key=")
@Import(PqrsStatisticsResourceIT.ObserveAggregations.class)
class PqrsStatisticsResourceIT {

    private static final String BASE = "/api/reports/gestion-pqrsd";
    private static final String START = "2004-02-01";
    private static final String END = "2004-03-31";
    private static final ObjectId OFFICE = new ObjectId();
    private static final String OTHER = "statistics-test-office";
    private static final List<ObjectId> ROOT_IDS = List.of(new ObjectId(), new ObjectId(), new ObjectId());
    private static final AtomicInteger AGGREGATIONS = new AtomicInteger();

    @TempDir
    Path directory;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private PqrsStatisticsService statistics;

    @TestConfiguration(proxyBeanMethods = false)
    static class ObserveAggregations {

        @Bean
        MongoClientSettingsBuilderCustomizer observeStatisticsQueries() {
            return builder ->
                builder.addCommandListener(
                    new CommandListener() {
                        @Override
                        public void commandStarted(CommandStartedEvent event) {
                            if (
                                "aggregate".equals(event.getCommandName()) &&
                                "pqrs".equals(event.getCommand().getString("aggregate").getValue())
                            ) {
                                AGGREGATIONS.incrementAndGet();
                            }
                        }
                    }
                );
        }
    }

    @BeforeEach
    void seed() {
        mongo
            .getCollection("oficina")
            .insertMany(
                List.of(new Document("_id", OFFICE).append("nombre", "Equipo A"), new Document("_id", OTHER).append("nombre", "Equipo B"))
            );
        mongo.getCollection("pqrs").insertMany(List.of(root(0, OFFICE, "REQUEST"), root(1, OFFICE, "DEMAND"), root(2, OTHER, "REQUEST")));
        AGGREGATIONS.set(0);
    }

    @AfterEach
    void clean() {
        mongo.getCollection("pqrs").deleteMany(new Document("_id", new Document("$in", ROOT_IDS)));
        mongo.getCollection("oficina").deleteMany(new Document("_id", new Document("$in", List.of(OFFICE, OTHER, OFFICE.toHexString()))));
    }

    private static Document root(int cursor, Object office, String type) {
        return new Document("_id", ROOT_IDS.get(cursor))
            .append("fecha_creacion", Date.from(Instant.parse("2004-02-29T18:00:00Z")))
            .append("type", type)
            .append("estado", "PENDIENTE")
            .append("oficinaResponder", new DBRef("oficina", office))
            .append("requester_email", "synthetic-private")
            .append("access_token", "synthetic-private-" + cursor)
            .append("descripcion", "synthetic-private");
    }

    @Test
    void deniesAnonymousRequestsBeforeAggregation() throws Exception {
        mvc.perform(valid()).andExpect(status().isUnauthorized());
        mvc.perform(export(START, END, "INSTITUTIONAL", null)).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/configuration")).andExpect(status().isUnauthorized());
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, FUNCTIONARY, FRONT_DESK_CS, "ROLE_OTHER" })
    void deniesProfilesWithoutStatisticalAccess(String role) throws Exception {
        var identity = user("synthetic").authorities(new SimpleGrantedAuthority(role));
        mvc.perform(valid().with(identity)).andExpect(status().isForbidden());
        mvc.perform(export(START, END, "INSTITUTIONAL", null).with(identity)).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/configuration").with(identity)).andExpect(status().isForbidden());
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void serializesInstitutionalStatisticsFromTheSameEngine() throws Exception {
        ObjectNode expected = (ObjectNode) json.readTree(
            json.writeValueAsBytes(
                statistics.calculate(new PqrsStatisticsFilter(LocalDate.parse(START), LocalDate.parse(END), Scope.INSTITUTIONAL, null))
            )
        );
        AGGREGATIONS.set(0);
        String body = mvc
            .perform(valid())
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.filter.start").value(START))
            .andExpect(jsonPath("$.filter.end").value(END))
            .andExpect(jsonPath("$.filter.scope").value("INSTITUTIONAL"))
            .andExpect(jsonPath("$.filter.officeId").value((Object) null))
            .andExpect(jsonPath("$.period.zone").value("America/Bogota"))
            .andExpect(jsonPath("$.period.startInclusive").value("2004-02-01T05:00:00Z"))
            .andExpect(jsonPath("$.period.endExclusive").value("2004-04-01T05:00:00Z"))
            .andExpect(jsonPath("$.period.effectiveCutoff").value("2004-04-01T05:00:00Z"))
            .andExpect(jsonPath("$.received").value(3))
            .andExpect(jsonPath("$.months[0].month").value("2004-02"))
            .andExpect(jsonPath("$.months[0].variationPercent").value((Object) null))
            .andExpect(jsonPath("$.months[1].received").value(0))
            .andExpect(jsonPath("$.months[1].currentOffices").isEmpty())
            .andExpect(jsonPath("$.types[?(@.code == 'DEMAND')].percent").value(33.3))
            .andExpect(jsonPath("$.deferredIndicators.IND-09").value("SOURCE_UNAVAILABLE"))
            .andExpect(jsonPath("$.coverage.warnings").isEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(AGGREGATIONS.get()).isEqualTo(1);
        assertThat(body).doesNotContain("synthetic-private", "requester_email", "access_token", ROOT_IDS.get(0).toHexString());
        ObjectNode actual = (ObjectNode) json.readTree(body);
        Instant.parse(actual.withObject("/period").remove("observedAt").asText());
        expected.withObject("/period").remove("observedAt");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @WithMockUser(authorities = PQRSD_REPORTS)
    void isolatesOfficesAndProvidesConfigurationWithoutCaseReads() throws Exception {
        mvc
            .perform(get(BASE + "/configuration"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.scopes[0]").value("INSTITUTIONAL"))
            .andExpect(jsonPath("$.scopes[1]").value("OFFICE"))
            .andExpect(jsonPath("$.zone").value("America/Bogota"))
            .andExpect(jsonPath("$.maxPeriodDays").value(366))
            .andExpect(jsonPath("$.availableIndicators.length()").value(7));
        assertThat(AGGREGATIONS.get()).isZero();
        mvc
            .perform(preview(START, END, "OFFICE", OFFICE.toHexString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(2))
            .andExpect(jsonPath("$.currentOffices.length()").value(1))
            .andExpect(jsonPath("$.currentOffices[0].id").value(OFFICE.toHexString()));
        mvc
            .perform(preview(START, END, "OFFICE", OTHER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.received").value(1))
            .andExpect(jsonPath("$.currentOffices[0].id").value(OTHER));
        assertThat(AGGREGATIONS.get()).isEqualTo(2);
    }

    @Test
    @WithMockUser(authorities = { FUNCTIONARY, PQRSD_REPORTS })
    void combinesAuthoritiesWithoutOperationalOfficeRequirement() throws Exception {
        mvc.perform(valid()).andExpect(status().isOk()).andExpect(jsonPath("$.received").value(3));
    }

    @Test
    @WithMockUser(authorities = PQRSD_REPORTS)
    void rejectsAmbiguousBsonOfficeBeforeAggregation() throws Exception {
        mongo.getCollection("oficina").insertOne(new Document("_id", OFFICE.toHexString()));
        mvc
            .perform(preview(START, END, "OFFICE", OFFICE.toHexString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknownOrAmbiguousOffice"));
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @ParameterizedTest
    @MethodSource("invalidFilters")
    @WithMockUser(authorities = PQRSD_REPORTS)
    void rejectsInvalidFiltersBeforeAggregation(String start, String end, String scope, String officeId, String errorCode)
        throws Exception {
        mvc
            .perform(preview(start, end, scope, officeId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error." + errorCode));
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @Test
    @WithMockUser(authorities = PQRSD_REPORTS)
    void rejectsRepeatedScopeAndAccepts366CivilDays() throws Exception {
        mvc.perform(valid().param("scope", "OFFICE")).andExpect(status().isBadRequest());
        assertThat(AGGREGATIONS.get()).isZero();
        mvc
            .perform(preview("2004-01-01", "2004-12-31", "INSTITUTIONAL", null))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.months.length()").value(12));
    }

    @Test
    @WithMockUser(authorities = PQRSD_REPORTS)
    void preservesLegacyExcelDownload() throws Exception {
        var id = new ObjectId();
        var reports = mongo.getCollection("informe_pqrs");
        reports.insertOne(
            new Document("_id", id)
                .append("fecha_inicio", Date.from(Instant.parse("2004-02-01T05:00:00Z")))
                .append("fecha_fin", Date.from(Instant.parse("2004-04-01T05:00:00Z")))
                .append("total_pqrs", 3)
                .append("total_resueltas", 0)
                .append("total_pendientes", 3)
        );
        try {
            mvc
                .perform(get("/api/reports/pqrs/{id}/download", id.toHexString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            assertThat(AGGREGATIONS.get()).isZero();
        } finally {
            reports.deleteOne(new Document("_id", id));
        }
    }

    @Test
    void returnsSanitizedProblemsWhenStatisticsStorageFails() throws Exception {
        var failingStatistics = mock(PqrsStatisticsService.class);
        when(failingStatistics.calculate(any()))
            .thenThrow(new QueryTimeoutException("synthetic internal query"))
            .thenThrow(new MongoException("synthetic internal server"));
        var standaloneMvc = MockMvcBuilders.standaloneSetup(
            new PqrsStatisticsResource(failingStatistics, new PqrsStatisticsWorkbook())
        ).build();
        for (String detail : List.of("The statistics query exceeded its time limit", "Statistics are temporarily unavailable")) {
            var response = standaloneMvc
                .perform(valid())
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.detail").value(detail))
                .andReturn()
                .getResponse();
            assertThat(response.getContentAsString()).doesNotContain("synthetic internal", "stackTrace");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, PQRSD_REPORTS })
    void exportsAuthorizedXlsxWithTheSameOfficeCountsAndNoCaseData(String role) throws Exception {
        var identity = user("synthetic").authorities(new SimpleGrantedAuthority(role));
        var preview = mvc
            .perform(preview(START, END, "OFFICE", OFFICE.toHexString()).with(identity))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
        var expected = json.readTree(preview.getContentAsByteArray());
        var xlsx = mvc
            .perform(export(START, END, "OFFICE", OFFICE.toHexString()).with(identity))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(
                header().string("Content-Disposition", "attachment; filename=\"Informe_PQRSD_Dependencia_" + START + "_" + END + ".xlsx\"")
            )
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andReturn()
            .getResponse();
        Path file = Files.write(directory.resolve("report.xlsx"), xlsx.getContentAsByteArray());
        try (var workbook = new XSSFWorkbook(OPCPackage.open(new ZipFileZipEntrySource(new ZipFile(file.toFile()))))) {
            assertThat(workbook.getSheetAt(1).getRow(4).getCell(1).getNumericCellValue()).isEqualTo(
                expected.at("/months/0/received").asInt()
            );
            assertThat(workbook.getSheetAt(1).getRow(6).getCell(1).getNumericCellValue()).isEqualTo(expected.get("received").asInt());
            assertThat(workbook.getSheetAt(4).getRow(4).getCell(1).getNumericCellValue()).isEqualTo(2);
            assertThat(workbook.getSheetAt(4).getRow(5).getCell(0).getStringCellValue()).isEqualTo("Total del periodo");
            for (var tab : workbook) for (var row : tab) for (var cell : row) {
                assertThat(cell.toString()).doesNotContain("synthetic-private", ROOT_IDS.get(0).toHexString());
            }
        }
        assertThat(AGGREGATIONS.get()).isEqualTo(2);
    }

    @Test
    @WithMockUser(authorities = { PQRSD_REPORTS, FUNCTIONARY })
    void rejectsInvalidExportFiltersBeforeAggregation() throws Exception {
        mongo.getCollection("oficina").insertOne(new Document("_id", OFFICE.toHexString()));
        mvc
            .perform(export(START, END, "OFFICE", OFFICE.toHexString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("error.unknownOrAmbiguousOffice"));
        mvc.perform(export(START, END, "OFFICE", "missing")).andExpect(status().isBadRequest());
        mvc.perform(export(START, END, "OFFICE", null)).andExpect(status().isBadRequest());
        mvc.perform(export(START, END, "INSTITUTIONAL", null).param("start", START)).andExpect(status().isBadRequest());
        mvc.perform(export("2000-01-01", END, "INSTITUTIONAL", null)).andExpect(status().isBadRequest());
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @Test
    void reportsQueryAndWorkbookErrorsWithoutSendingAnAttachment() throws Exception {
        var stats = mock(PqrsStatisticsService.class);
        var book = mock(PqrsStatisticsWorkbook.class);
        var standalone = MockMvcBuilders.standaloneSetup(new PqrsStatisticsResource(stats, book)).build();
        when(stats.calculate(any())).thenThrow(new QueryTimeoutException("private query detail"));
        standalone
            .perform(export(START, END, "INSTITUTIONAL", null))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().doesNotExist("Content-Disposition"))
            .andExpect(jsonPath("$.detail").value("The statistics query exceeded its time limit"));
        var filter = new PqrsStatisticsFilter(LocalDate.parse(START), LocalDate.parse(END), Scope.INSTITUTIONAL, null);
        org.mockito.Mockito.reset(stats);
        when(stats.calculate(any())).thenReturn(
            new co.edu.itp.svu.service.dto.PqrsStatistics(
                filter,
                null,
                null,
                0,
                List.of(),
                List.of(),
                java.util.Map.of(),
                List.of(),
                java.util.Map.of()
            )
        );
        when(book.generate(any())).thenThrow(new IOException("private file detail"));
        standalone
            .perform(export(START, END, "INSTITUTIONAL", null))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().doesNotExist("Content-Disposition"))
            .andExpect(jsonPath("$.detail").value("The Excel report could not be generated"));
    }

    static Stream<Arguments> invalidFilters() {
        return Stream.of(
            Arguments.of(null, END, "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of(START, null, "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of("2004-2-01", END, "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of("2004-02-30", END, "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of(END, START, "INSTITUTIONAL", null, "invalidPeriod"),
            Arguments.of(START, LocalDate.now(ZoneId.of("America/Bogota")).plusDays(1).toString(), "INSTITUTIONAL", null, "futurePeriod"),
            Arguments.of("2004-01-01", "2005-01-01", "INSTITUTIONAL", null, "periodTooLong"),
            Arguments.of("0000-01-01", "0000-01-02", "INSTITUTIONAL", null, "invalidPeriod"),
            Arguments.of("-999999999-01-01", END, "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of(START, "+999999999-12-31", "INSTITUTIONAL", null, "invalidDate"),
            Arguments.of(START, END, null, null, "invalidScope"),
            Arguments.of(START, END, "GLOBAL", null, "invalidScope"),
            Arguments.of(START, END, "OFFICE", null, "officeRequired"),
            Arguments.of(START, END, "OFFICE", "", "officeRequired"),
            Arguments.of(START, END, "OFFICE", "missing", "unknownOrAmbiguousOffice"),
            Arguments.of(START, END, "OFFICE", "x".repeat(129), "invalidOffice"),
            Arguments.of(START, END, "INSTITUTIONAL", "", "unexpectedOffice")
        );
    }

    private static MockHttpServletRequestBuilder valid() {
        return preview(START, END, "INSTITUTIONAL", null);
    }

    private static MockHttpServletRequestBuilder preview(String start, String end, String scope, String officeId) {
        return filtered("/preview", start, end, scope, officeId);
    }

    private static MockHttpServletRequestBuilder export(String start, String end, String scope, String officeId) {
        return filtered("/export/xlsx", start, end, scope, officeId);
    }

    private static MockHttpServletRequestBuilder filtered(String path, String start, String end, String scope, String officeId) {
        var request = get(BASE + path);
        if (start != null) request.param("start", start);
        if (end != null) request.param("end", end);
        if (scope != null) request.param("scope", scope);
        if (officeId != null) request.param("officeId", officeId);
        return request;
    }

    @DynamicPropertySource
    static void narrativeKeys(DynamicPropertyRegistry registry) {
        byte[] synthetic = new byte[32];
        new SecureRandom().nextBytes(synthetic);
        String ring = "test:" + Base64.getEncoder().encodeToString(synthetic);
        registry.add("svu.pqrs-statistics.receipt.active-kid", () -> "test");
        registry.add("svu.pqrs-statistics.receipt.keys", () -> ring);
    }

    @Autowired
    private JwtEncoder encoder;

    private String narrativeAuthorization(String roles) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
            .subject("synthetic-analyst")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(120))
            .claim("auth", roles)
            .build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), claims)).getTokenValue();
    }

    private static final String BODY = "{\"filter\":{\"start\":\"2004-02-01\",\"end\":\"2004-03-31\",\"scope\":\"INSTITUTIONAL\"}}";

    private MockHttpServletRequestBuilder narrative(String json) {
        return post(BASE + "/narrative-previews").contentType("application/json").content(json);
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, PQRSD_REPORTS, FUNCTIONARY + " " + PQRSD_REPORTS })
    void issuesFromActualAuthenticatedCalculation(String roles) throws Exception {
        mvc
            .perform(narrative(BODY).header("Authorization", narrativeAuthorization(roles)))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.receipt").isString())
            .andExpect(jsonPath("$.expiresAt").isString())
            .andExpect(jsonPath("$.modelVersion").value(1))
            .andExpect(jsonPath("$.statistics.received").value(3))
            .andExpect(jsonPath("$.availableIndicators.length()").value(7));
        assertThat(AGGREGATIONS.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", USER, FUNCTIONARY, FRONT_DESK_CS })
    void preventsUnauthorizedNarrativePreview(String roles) throws Exception {
        var req = narrative(BODY);
        if (!roles.isEmpty()) req.header("Authorization", narrativeAuthorization(roles));
        mvc
            .perform(req)
            .andExpect(status().is(roles.isEmpty() ? 401 : 403))
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @Test
    void enforcesLocalStrictJsonAndBodyLimits() throws Exception {
        String authorization = narrativeAuthorization(PQRSD_REPORTS);
        for (String json : List.of(
            BODY + BODY,
            BODY.replace("\"end\":", "\"start\":\"2004-02-01\",\"end\":"),
            BODY.replace("\"filter\":", "\"filter\":{},\"filter\":"),
            BODY.replace("\"scope\":", "\"unknown\":7,\"scope\":"),
            BODY.replace("\"INSTITUTIONAL\"", "42"),
            BODY.replace("\"INSTITUTIONAL\"", "null"),
            "{\"filter\":[[[[[[]]]]]]}",
            "{\"statistics\":{},\"filter\":{}}"
        )) {
            mvc
                .perform(narrative(json).header("Authorization", authorization))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("error.invalidExportRequest"))
                .andExpect(header().string("Cache-Control", "no-store"));
        }
        mvc
            .perform(narrative("").content(new byte[] { (byte) 0xff }).header("Authorization", authorization))
            .andExpect(status().isBadRequest());
        mvc
            .perform(narrative(BODY).header("Content-Encoding", "gzip").header("Authorization", authorization))
            .andExpect(status().isUnsupportedMediaType());
        mvc
            .perform(narrative(BODY).contentType("text/plain").header("Authorization", authorization))
            .andExpect(status().isUnsupportedMediaType());
        mvc.perform(narrative(BODY + " ".repeat(262145)).header("Authorization", authorization)).andExpect(status().isPayloadTooLarge());
        assertThat(AGGREGATIONS.get()).isZero();
    }

    @Test
    void enforcesInclusiveByteBudgetOnBodiesOfUnknownLength() {
        var service = mock(co.edu.itp.svu.service.report.PqrsStatisticsNarrativeService.class);
        var resource = new PqrsStatisticsNarrativeResource(service);
        var request = new org.springframework.mock.web.MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        byte[] exact =
            (BODY + " ".repeat(PqrsStatisticsNarrativeRequest.MAX_BODY - BODY.length())).getBytes(
                    java.nio.charset.StandardCharsets.US_ASCII
                );
        request.setContent(exact);
        assertThat(resource.create(request).getStatusCode().value()).isEqualTo(200);
        request.setContent(java.util.Arrays.copyOf(exact, exact.length + 1));
        assertThatThrownBy(() -> resource.create(request)).hasMessage("exportBodyTooLarge");
        org.mockito.Mockito.verify(service).preview(any());
    }
}
