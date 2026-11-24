package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class PqrsStatisticsServiceTest {

    private final LocalDate today = LocalDate.of(2024, 2, 29);
    private final Instant now = Instant.parse("2024-02-29T16:00:00Z");
    private final PqrsStatisticsRepository repository = mock(PqrsStatisticsRepository.class);
    private final PqrsAuthorizationService auth = mock(PqrsAuthorizationService.class);
    private final PqrsStatisticsService service = new PqrsStatisticsService(repository, auth, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void rejectsImplicitOrContradictoryScopeAndInvalidPeriod() {
        for (String invalid : new String[] { null, "", " " }) assertThatIllegalArgumentException()
            .isThrownBy(() -> new PqrsStatisticsFilter(today, today, Scope.OFFICE, invalid));
        assertThatIllegalArgumentException().isThrownBy(() -> new PqrsStatisticsFilter(today, today, Scope.INSTITUTIONAL, "A"));
        assertThatNullPointerException().isThrownBy(() -> new PqrsStatisticsFilter(today, today, null, null));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PqrsStatisticsFilter(today, today.minusDays(1), Scope.INSTITUTIONAL, null));
        assertThatIllegalArgumentException()
            .isThrownBy(() -> service.calculate(new PqrsStatisticsFilter(today, today.plusDays(1), Scope.INSTITUTIONAL, null)));
        verifyNoInteractions(repository);
    }

    @Test
    void deniesBeforeReadingData() {
        doThrow(new AccessDeniedException("Denied")).when(auth).requireStatisticalAccess();
        assertThatThrownBy(() -> service.calculate(new PqrsStatisticsFilter(today, today, Scope.INSTITUTIONAL, null))).isInstanceOf(
            AccessDeniedException.class
        );
        verifyNoInteractions(repository);
    }

    @Test
    void partialMonthsHaveNoComparableVariationAndCurrentDayIsClipped() {
        when(repository.institutional(any())).thenReturn(List.of());
        var result = service.calculate(new PqrsStatisticsFilter(LocalDate.of(2023, 12, 15), today, Scope.INSTITUTIONAL, null));
        assertThat(result.period().effectiveCutoff()).isEqualTo(now);
        assertThat(result.period().endExclusive()).isEqualTo(Instant.parse("2024-03-01T05:00:00Z"));
        assertThat(result.coverage().warnings()).contains("PROVISIONAL_PERIOD");
        assertThat(result.months()).extracting("comparison").containsExactly("PARTIAL_MONTH", "NO_COMPARABLE_BASE", "PARTIAL_MONTH");
        assertThat(result.types()).allMatch(row -> row.denominator() == 0 && row.percent() == null);
        assertThat(result.deferredIndicators()).hasSize(12);
    }
}
