package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.edu.itp.svu.aop.logging.LoggingAspect;
import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.repository.PqrsStatisticsRepository.ResolvedOffice;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.*;
import co.edu.itp.svu.service.dto.PqrsStatistics.*;
import co.edu.itp.svu.service.report.PqrsStatisticsService.Observation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PqrsStatisticsReceiptTest {

    static final Instant NOW = Instant.parse("2024-02-02T05:00:00Z");
    static final PqrsStatisticsFilter FILTER = new PqrsStatisticsFilter(
        LocalDate.parse("2024-02-01"),
        LocalDate.parse("2024-02-01"),
        PqrsStatisticsFilter.Scope.OFFICE,
        "á"
    );
    static final String PREFIX = "svu.pqrs-statistics.receipt.";
    final String oldKey = syntheticKey();
    final String newKey = syntheticKey();
    final PqrsStatisticsReceipts signer = ring("old", "old:" + oldKey);
    final Jwt jwt = jwt("sample", "synthetic-credential", NOW.plusSeconds(3600));
    final byte[] digest = PqrsStatisticsProjection.digest(new byte[] { 7 });

    static String syntheticKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    static PqrsStatisticsReceipts ring(String kid, String keys) {
        return new PqrsStatisticsReceipts(
            new MockEnvironment().withProperty(PREFIX + "active-kid", kid).withProperty(PREFIX + "keys", keys)
        );
    }

    static Jwt jwt(String subject, String credential, Instant expiry) {
        return Jwt.withTokenValue(credential)
            .header("alg", "HS512")
            .subject(subject)
            .issuedAt(expiry.minusSeconds(7200))
            .expiresAt(expiry)
            .build();
    }

    private String issue() {
        return signer.issue(jwt, FILTER, NOW, NOW, digest).receipt();
    }

    @Test
    void canonicalProjectionMatchesIndependentUtf8VectorAndSha256() throws Exception {
        var period = new PqrsStatistics.Period("America/Bogota", NOW.minusSeconds(86400), NOW, NOW, NOW);
        var month = new PqrsStatistics.Month(YearMonth.of(2024, 2), 0, false, null, "PARTIAL_MONTH", null, Map.of(), List.of());
        var s = new PqrsStatistics(
            FILTER,
            period,
            new Coverage("PARTIAL", List.of("Z", "A"), List.of()),
            0,
            List.of(month),
            List.of(new TypeShare("REQUEST", 0, 0, null)),
            Map.of("PENDING", 0L),
            List.of(),
            Map.of()
        );
        byte[] result = PqrsStatisticsProjection.content(
            new Observation(s, new ResolvedOffice("string", "á", "Área 🌳")),
            List.of("IND-01")
        );
        try (var vector = getClass().getResourceAsStream("/statistics/projection-v1.txt")) {
            assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(
                new String(vector.readAllBytes(), StandardCharsets.UTF_8).stripTrailing()
            );
        }
        assertThat(HexFormat.of().formatHex(PqrsStatisticsProjection.digest(result))).isEqualTo(
            "c593f93b0a2ba3c50c36c2e0fce95ba8a3491fa703cdf96a4a5befdf3485485f"
        );
    }

    @Test
    void canonicalObjectsSortKeysAndPreserveNumbersNullsAndScalarValues() {
        var map = new LinkedHashMap<String, Object>();
        map.put("z", null);
        map.put("x", List.of());
        map.put("b", new BigDecimal("2.100"));
        map.put("a", 0L);
        assertThat(new String(PqrsStatisticsProjection.bytes(map), StandardCharsets.UTF_8)).isEqualTo(
            "{\"a\":\"0\",\"b\":\"2.1\",\"x\":[],\"z\":null}"
        );
        assertThat(HexFormat.of().formatHex(PqrsStatisticsProjection.digest(PqrsStatisticsProjection.bytes(map)))).isEqualTo(
            "5effe02cce23173cb3c54247bdf8c62c2d8214ab1fb6229dcb5b6376e8e708d6"
        );
        assertThat(PqrsStatisticsProjection.bytes(map)).isEqualTo(PqrsStatisticsProjection.bytes(new TreeMap<>(map)));
        assertThatThrownBy(() -> PqrsStatisticsProjection.bytes(new Object())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PqrsStatisticsProjection.bytes(String.valueOf(Character.MIN_SURROGATE))).isInstanceOf(
            IllegalStateException.class
        );
    }

    @Test
    void differencesInInterpretationChangeDigestButInputOrderingDoesNot() {
        var filter = FILTER;
        var period = new PqrsStatistics.Period("America/Bogota", NOW.minusSeconds(86400), NOW, NOW, NOW);
        var office = new OfficeCount("ASSIGNED", "string", "á", "Área", 1);
        var other = new OfficeCount("ASSIGNED", "objectId", "abcd", "Área", 2);
        var month = new PqrsStatistics.Month(
            YearMonth.of(2024, 2),
            3,
            true,
            2L,
            "CALCULATED",
            new BigDecimal("50.00"),
            Map.of("B", 2L, "A", 1L),
            List.of(office, other)
        );
        var type = new TypeShare("A", 1, 3, new BigDecimal("33.3"));
        var second = new TypeShare("B", 2, 3, new BigDecimal("66.7"));
        var base = new PqrsStatistics(
            filter,
            period,
            new Coverage("OBSERVED", List.of("X", "Y"), List.of("L")),
            3,
            List.of(month),
            List.of(type, second),
            Map.of("S", 3L),
            List.of(office, other),
            Map.of("IND-08", "P")
        );
        var reorderedMonth = new PqrsStatistics.Month(
            month.month(),
            3,
            true,
            2L,
            "CALCULATED",
            new BigDecimal("50.0"),
            Map.of("A", 1L, "B", 2L),
            List.of(other, office)
        );
        var reordered = new PqrsStatistics(
            filter,
            period,
            new Coverage("OBSERVED", List.of("Y", "X"), List.of("L")),
            3,
            List.of(reorderedMonth),
            List.of(second, type),
            base.currentStates(),
            List.of(other, office),
            base.deferredIndicators()
        );
        var resolved = new ResolvedOffice("string", "á", "Área");
        byte[] original = PqrsStatisticsProjection.content(new Observation(base, resolved), List.of("I", "J"));
        assertThat(PqrsStatisticsProjection.content(new Observation(reordered, resolved), List.of("J", "I"))).isEqualTo(original);
        assertThat(PqrsStatisticsProjection.content(new Observation(base, resolved), List.of("I"))).isNotEqualTo(original);
        for (var duplicate : List.of(
            new PqrsStatistics(
                filter,
                period,
                base.coverage(),
                3,
                List.of(month, month),
                base.types(),
                base.currentStates(),
                base.currentOffices(),
                base.deferredIndicators()
            ),
            new PqrsStatistics(
                filter,
                period,
                base.coverage(),
                3,
                base.months(),
                List.of(type, type),
                base.currentStates(),
                base.currentOffices(),
                base.deferredIndicators()
            ),
            new PqrsStatistics(
                filter,
                period,
                base.coverage(),
                3,
                base.months(),
                base.types(),
                base.currentStates(),
                List.of(office, office),
                base.deferredIndicators()
            )
        )) assertThatThrownBy(() -> PqrsStatisticsProjection.content(new Observation(duplicate, resolved), List.of())).hasMessage(
            "Duplicate statistical key"
        );
    }

    @Test
    void receiptsAreRandomBoundAndVerifiableAcrossRotation() {
        String receipt = issue();
        assertThat(issue()).isNotEqualTo(receipt);
        var rotated = ring("new", "new:" + newKey + ",old:" + oldKey);
        var verified = rotated.authenticate(receipt, jwt, FILTER, NOW);
        rotated.compare(verified, digest);
        assertThat(verified.observation()).isEqualTo(NOW);
        assertThat(verified.expiry()).isEqualTo(NOW.plusSeconds(1800));
        assertThatThrownBy(() -> rotated.compare(verified, new byte[32])).hasMessage("statisticsChanged");
        assertThatThrownBy(() -> ring("new", "new:" + newKey).authenticate(receipt, jwt, FILTER, NOW)).hasMessage("previewNotFound");
        assertThatThrownBy(() -> rotated.authenticate(receipt, jwt("another", jwt.getTokenValue(), jwt.getExpiresAt()), FILTER, NOW)
        ).hasMessage("previewNotFound");
        assertThatThrownBy(() -> rotated.authenticate(receipt, jwt("sample", "another", jwt.getExpiresAt()), FILTER, NOW.plusSeconds(1800))
        ).hasMessage("previewNotFound");
        var another = new PqrsStatisticsFilter(FILTER.start(), FILTER.end(), PqrsStatisticsFilter.Scope.INSTITUTIONAL, null);
        assertThatThrownBy(() -> rotated.authenticate(receipt, jwt, another, NOW)).hasMessage("previewFilterMismatch");
        assertThatThrownBy(() -> rotated.authenticate(receipt, jwt, FILTER, NOW.plusSeconds(1800))).hasMessage("previewExpired");
        var shortJwt = jwt("sample", "short", NOW.plusSeconds(9));
        assertThat(signer.issue(shortJwt, FILTER, NOW, NOW, digest).expiresAt()).isEqualTo(shortJwt.getExpiresAt());
    }

    @Test
    void rejectsMalformedTamperedAndOversizeReferences() {
        String receipt = issue();
        String[] parts = receipt.split("\\.");
        List<String> altered = new ArrayList<>(
            List.of("", receipt + "=", receipt + "\n", "a".repeat(4097), "é." + parts[1] + "." + parts[2])
        );
        for (int i = 0; i < 3; i++) {
            var changed = parts.clone();
            changed[i] = (changed[i].startsWith("A") ? "B" : "A") + changed[i].substring(1);
            altered.add(String.join(".", changed));
        }
        for (String value : altered) assertThatThrownBy(() -> signer.authenticate(value, jwt, FILTER, NOW)).hasMessage("previewNotFound");
    }

    @Test
    void rejectsEvenAuthenticatedWrongAudienceAndObsoleteMethodology() throws Exception {
        String[] parts = issue().split("\\.");
        ObjectNode payload = (ObjectNode) new ObjectMapper().readTree(Base64.getUrlDecoder().decode(parts[1]));
        String method = signed(parts[0], payload.put("method", 2));
        assertThatThrownBy(() -> signer.authenticate(method, jwt, FILTER, NOW)).hasMessage("previewVersionChanged");
        String audience = signed(parts[0], payload.put("method", 1).put("aud", "other"));
        assertThatThrownBy(() -> signer.authenticate(audience, jwt, FILTER, NOW)).hasMessage("previewNotFound");
    }

    private String signed(String header, ObjectNode payload) throws Exception {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        String body = header + "." + encoder.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(oldKey), "HmacSHA256"));
        return (
            body +
            "." +
            encoder.encodeToString(
                mac.doFinal(("[\"svu.pqrs.receipt/1\",\"signature\",\"" + body + "\"]").getBytes(StandardCharsets.UTF_8))
            )
        );
    }

    @Test
    void invalidModuleConfigurationFailsClosedOnlyWhenUsed() {
        for (var env : List.of(
            new MockEnvironment(),
            new MockEnvironment().withProperty(PREFIX + "active-kid", "old").withProperty(PREFIX + "keys", "old:AA=="),
            new MockEnvironment()
                .withProperty(PREFIX + "active-kid", "old")
                .withProperty(PREFIX + "keys", "old:" + oldKey)
                .withProperty(PREFIX + "ttl-minutes", "61"),
            new MockEnvironment()
                .withProperty(PREFIX + "active-kid", "old")
                .withProperty(PREFIX + "keys", "old:" + oldKey)
                .withProperty("jhipster.security.authentication.jwt.base64-secret", oldKey)
        )) {
            var disabled = new PqrsStatisticsReceipts(env);
            assertThatThrownBy(disabled::requireConfigured).hasMessage("exportUnavailable");
        }
    }

    @Test
    void developmentAdviceDoesNotRecordSuccessfulPreviewOrFailedReceiptAtDebug() {
        var logger = (Logger) LoggerFactory.getLogger(PqrsStatisticsNarrativeService.class);
        Level original = logger.getLevel();
        var capture = new ListAppender<ILoggingEvent>();
        capture.start();
        logger.addAppender(capture);
        logger.setLevel(Level.DEBUG);
        var environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        var auth = new PqrsAuthorizationService(null, null);
        var repo = mock(PqrsStatisticsRepository.class);
        when(repo.institutional(any())).thenReturn(List.of());
        var target = new PqrsStatisticsNarrativeService(
            new PqrsStatisticsService(repo, auth),
            auth,
            signer,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var factory = new AspectJProxyFactory(target);
        factory.addAspect(new LoggingAspect(environment));
        PqrsStatisticsNarrativeService proxy = factory.getProxy();
        var filter = new PqrsStatisticsFilter(FILTER.start(), FILTER.end(), PqrsStatisticsFilter.Scope.INSTITUTIONAL, null);
        try {
            SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_PQRSD_REPORTS"))));
            logger.debug("capture-enabled");
            var preview = proxy.preview(filter);
            proxy.check(filter, preview.receipt());
            assertThatThrownBy(() -> proxy.check(filter, "SYNTHETIC-PRIVATE-RECEIPT")).hasMessage("previewNotFound");
            when(repo.institutional(any())).thenThrow(new com.mongodb.MongoException("SYNTHETIC-PRIVATE-DB-ERROR"));
            assertThatThrownBy(() -> proxy.check(filter, preview.receipt())).hasMessage("exportUnavailable").hasNoCause();
            assertThat(capture.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly("capture-enabled");
            assertThat(preview.toString()).doesNotContain(preview.receipt(), jwt.getTokenValue(), "IND-01");
            verify(repo, times(3)).institutional(any());
        } finally {
            logger.detachAppender(capture);
            logger.setLevel(original);
            capture.stop();
            SecurityContextHolder.clearContext();
        }
    }
}
