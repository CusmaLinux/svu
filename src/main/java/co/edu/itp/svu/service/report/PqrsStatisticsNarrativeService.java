package co.edu.itp.svu.service.report;

import static co.edu.itp.svu.service.report.PqrsStatisticsProjection.*;

import co.edu.itp.svu.repository.PqrsStatisticsRepository.ResolvedOffice;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import com.mongodb.MongoException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class PqrsStatisticsNarrativeService {

    private final PqrsStatisticsService engine;
    private final PqrsAuthorizationService authorization;
    private final PqrsStatisticsReceipts receipts;
    private final Clock clock;

    @Autowired
    PqrsStatisticsNarrativeService(PqrsStatisticsService engine, PqrsAuthorizationService authorization, PqrsStatisticsReceipts receipts) {
        this(engine, authorization, receipts, Clock.systemUTC());
    }

    PqrsStatisticsNarrativeService(
        PqrsStatisticsService engine,
        PqrsAuthorizationService authorization,
        PqrsStatisticsReceipts receipts,
        Clock clock
    ) {
        this.engine = engine;
        this.authorization = authorization;
        this.receipts = receipts;
        this.clock = clock;
    }

    public record Preview(
        String receipt,
        Instant expiresAt,
        int modelVersion,
        int methodologyVersion,
        int catalogVersion,
        ResolvedOffice resolvedOffice,
        List<String> availableIndicators,
        PqrsStatistics statistics
    ) {
        @Override
        public String toString() {
            return "Narrative preview";
        }
    }

    public record Checked(
        PqrsStatistics statistics,
        ResolvedOffice resolvedOffice,
        List<String> availableIndicators,
        Instant checkedAt,
        Instant expiresAt,
        int modelVersion,
        int methodologyVersion,
        int catalogVersion
    ) {
        @Override
        public String toString() {
            return "Verified statistics";
        }
    }

    public Preview preview(PqrsStatisticsFilter filter) {
        Jwt jwt = currentCredential();
        receipts.requireConfigured();
        var available = engine.configuration().availableIndicators();
        var observation = observe(filter, now());
        byte[] fingerprint = fingerprint(observation, available);
        var issued = receipts.issue(jwt, filter, observation.statistics().period().observedAt(), now(), fingerprint);
        return new Preview(
            issued.receipt(),
            issued.expiresAt(),
            MODEL,
            METHODOLOGY,
            CATALOG,
            observation.office(),
            available,
            observation.statistics()
        );
    }

    public Checked check(PqrsStatisticsFilter filter, String receipt) {
        Jwt jwt = currentCredential();
        Instant checking = now();
        engine.validatePeriod(filter, checking);
        var verified = receipts.authenticate(receipt, jwt, filter, checking);
        var available = engine.configuration().availableIndicators();
        var observation = observe(filter, verified.observation());
        receipts.compare(verified, fingerprint(observation, available));
        Instant checked = now();
        if (!checked.isBefore(jwt.getExpiresAt())) throw new PqrsStatisticsReceiptException(401, "unauthenticated");
        if (!checked.isBefore(verified.expiry())) throw new PqrsStatisticsReceiptException(410, "previewExpired");
        return new Checked(
            observation.statistics(),
            observation.office(),
            available,
            checked,
            verified.expiry(),
            MODEL,
            METHODOLOGY,
            CATALOG
        );
    }

    private PqrsStatisticsService.Observation observe(PqrsStatisticsFilter filter, Instant originalObservation) {
        try {
            return engine.observe(filter, originalObservation);
        } catch (MongoException | DataAccessException failure) {
            throw PqrsStatisticsReceiptException.unavailable();
        }
    }

    private static byte[] fingerprint(PqrsStatisticsService.Observation observation, List<String> available) {
        try {
            return digest(content(observation, available));
        } catch (IllegalStateException inconsistentProjection) {
            throw PqrsStatisticsReceiptException.unavailable();
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }

    String exportCredentialKey() {
        return java.util.HexFormat.of()
            .formatHex(digest(currentCredential().getTokenValue().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    void recheckValidity(PqrsStatisticsFilter filter, String receipt) {
        var credential = currentCredential();
        receipts.authenticate(receipt, credential, filter, now());
    }

    private Jwt currentCredential() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) throw new PqrsStatisticsReceiptException(
            401,
            "unauthenticated"
        );
        authorization.requireStatisticalAccess();
        Jwt jwt = token.getToken();
        if (
            jwt.getSubject() == null || jwt.getSubject().isBlank() || jwt.getExpiresAt() == null || !now().isBefore(jwt.getExpiresAt())
        ) throw new PqrsStatisticsReceiptException(401, "unauthenticated");
        return jwt;
    }
}
