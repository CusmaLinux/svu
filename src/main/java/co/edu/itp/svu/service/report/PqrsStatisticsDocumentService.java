package co.edu.itp.svu.service.report;

import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PqrsStatisticsDocumentService {

    private final PqrsStatisticsNarrativeService previews;
    private final Clock clock;

    @Autowired
    PqrsStatisticsDocumentService(PqrsStatisticsNarrativeService previews) {
        this(previews, Clock.systemUTC());
    }

    PqrsStatisticsDocumentService(PqrsStatisticsNarrativeService previews, Clock clock) {
        this.previews = previews;
        this.clock = clock;
    }

    public record ExportRequest(String receipt, PqrsStatisticsFilter filter, PqrsStatisticsNarrativeText narrative) {
        public ExportRequest {
            Objects.requireNonNull(receipt);
            Objects.requireNonNull(filter);
            Objects.requireNonNull(narrative);
        }
        @Override
        public String toString() {
            return "Narrative export request";
        }
    }

    public record Prepared(PqrsStatisticsDocument document, PqrsStatistics statistics, java.util.List<String> availableIndicators) {}

    public PqrsStatisticsDocument prepare(ExportRequest request) {
        return prepareSnapshot(request).document();
    }

    public Prepared prepareSnapshot(ExportRequest request) {
        var checked = previews.check(request.filter(), request.receipt());
        var document = PqrsStatisticsDocumentAssembler.assemble(
            checked,
            request.narrative(),
            clock.instant().truncatedTo(ChronoUnit.MILLIS)
        );
        return new Prepared(document, checked.statistics(), checked.availableIndicators());
    }
}
