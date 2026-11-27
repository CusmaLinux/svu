package co.edu.itp.svu.service.report;

import co.edu.itp.svu.domain.PqrsStatisticsReportAudit;
import co.edu.itp.svu.domain.PqrsStatisticsSavedReport;
import co.edu.itp.svu.repository.PqrsStatisticsRepository.ResolvedOffice;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.report.PqrsStatisticsDocumentService.ExportRequest;
import co.edu.itp.svu.service.report.PqrsStatisticsNarrativeService.Checked;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class PqrsStatisticsSavedReportService {

    private static final String REPORT_COLLECTION = "pqrs_statistics_saved_report";
    private static final String AUDIT_COLLECTION = "pqrs_statistics_report_audit";
    private final MongoTemplate mongo;
    private final ObjectMapper json;
    private final PqrsAuthorizationService authorization;
    private final PqrsStatisticsDocumentService documents;
    private final PqrsStatisticsWordExport word;
    private final PqrsStatisticsWorkbook workbook;
    private final Clock clock;

    @Autowired
    public PqrsStatisticsSavedReportService(
        MongoTemplate mongo,
        ObjectMapper json,
        PqrsAuthorizationService authorization,
        PqrsStatisticsDocumentService documents,
        PqrsStatisticsWordExport word,
        PqrsStatisticsWorkbook workbook
    ) {
        this(mongo, json, authorization, documents, word, workbook, Clock.systemUTC());
    }

    PqrsStatisticsSavedReportService(
        MongoTemplate mongo,
        ObjectMapper json,
        PqrsAuthorizationService authorization,
        PqrsStatisticsDocumentService documents,
        PqrsStatisticsWordExport word,
        PqrsStatisticsWorkbook workbook,
        Clock clock
    ) {
        this.mongo = mongo;
        this.json = json;
        this.authorization = authorization;
        this.documents = documents;
        this.word = word;
        this.workbook = workbook;
        this.clock = clock;
    }

    public record Summary(
        String id,
        String title,
        PqrsStatisticsFilter filter,
        ResolvedOffice office,
        Instant cutoff,
        Instant createdAt,
        Instant modifiedAt,
        Instant generatedAt,
        String createdBy,
        String modifiedBy,
        Long version,
        long received
    ) {}

    public record Page(List<Summary> reports, long total, int page, int size) {}

    public record Detail(
        String id,
        String title,
        PqrsStatisticsFilter filter,
        ResolvedOffice office,
        Instant cutoff,
        PqrsStatistics statistics,
        Map<String, String> narrative,
        String createdBy,
        String modifiedBy,
        Instant createdAt,
        Instant modifiedAt,
        Instant generatedAt,
        int modelVersion,
        int methodologyVersion,
        int catalogVersion,
        Long version
    ) {}

    public Detail create(ExportRequest request) {
        authorization.requireStatisticalAccess();
        String actor = actor();
        var prepared = documents.prepareSnapshot(request);
        var metadata = prepared.document().metadata();
        var report = new PqrsStatisticsSavedReport();
        Instant now = now();
        report.setId(metadata.id().toString());
        report.setTitle("Informe de gestión PQRSD · " + metadata.filter().start() + " a " + metadata.filter().end());
        report.setScope(metadata.filter().scope().name());
        report.setStartDate(metadata.filter().start());
        report.setEndDate(metadata.filter().end());
        report.setOfficeId(metadata.filter().officeId());
        if (metadata.office() != null) {
            report.setOfficeIdType(metadata.office().idType());
            report.setOfficeIdentity(metadata.office().id());
            report.setOfficeName(metadata.office().name());
        }
        report.setCutoff(metadata.period().effectiveCutoff());
        report.setStatisticsSnapshot(serialize(prepared.statistics()));
        report.setReceived(prepared.statistics().received());
        report.setNarrative(request.narrative().asMap());
        report.setAvailableIndicators(prepared.availableIndicators());
        report.setCheckedAt(metadata.checkedAt());
        report.setCreatedBy(actor);
        report.setModifiedBy(actor);
        report.setCreatedAt(now);
        report.setModifiedAt(now);
        report.setGeneratedAt(metadata.generatedAt());
        report.setModelVersion(metadata.modelVersion());
        report.setMethodologyVersion(metadata.methodologyVersion());
        report.setCatalogVersion(metadata.catalogVersion());
        report.setVersion(0L);
        mongo.insert(report, REPORT_COLLECTION);
        audit(report.getId(), actor, "CREATE", null, "SUCCESS");
        audit(report.getId(), actor, "GENERATE", null, "SUCCESS");
        return detail(report);
    }

    public Page list(PqrsStatisticsFilter.Scope scope, LocalDate start, LocalDate end, int page, int size) {
        authorization.requireStatisticalAccess();
        if (page < 0 || size < 1 || size > 50 || (start != null && end != null && start.isAfter(end))) throw new IllegalArgumentException(
            "Invalid saved report page filter"
        );
        Criteria criteria = Criteria.where("createdBy").is(actor());
        if (scope != null) criteria.and("scope").is(scope.name());
        if (start != null) criteria.and("endDate").gte(start);
        if (end != null) criteria.and("startDate").lte(end);
        var query = Query.query(criteria).with(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))));
        var reports = mongo.find(query, PqrsStatisticsSavedReport.class, REPORT_COLLECTION).stream().map(this::summary).toList();
        long total = mongo.count(Query.query(criteria), PqrsStatisticsSavedReport.class, REPORT_COLLECTION);
        return new Page(reports, total, page, size);
    }

    public Detail get(String id) {
        return detail(owned(id));
    }

    public Detail updateNarrative(String id, long expectedVersion, PqrsStatisticsNarrativeText narrative) {
        var report = owned(id);
        String actor = actor();
        if (!java.util.Objects.equals(report.getVersion(), expectedVersion)) {
            audit(id, actor, "UPDATE", null, "CONFLICT");
            throw new SavedReportConflictException();
        }
        report.setNarrative(narrative.asMap());
        report.setModifiedBy(actor);
        report.setModifiedAt(now());
        try {
            mongo.save(report, REPORT_COLLECTION);
        } catch (OptimisticLockingFailureException conflict) {
            audit(id, actor, "UPDATE", null, "CONFLICT");
            throw new SavedReportConflictException();
        }
        audit(id, actor, "UPDATE", null, "SUCCESS");
        return detail(report);
    }

    public PqrsStatisticsWordExport.File exportWord(String id) {
        var report = owned(id);
        String actor = actor();
        try {
            var file = word.generateSnapshot(() -> document(report));
            audit(id, actor, "GENERATE", "DOCX", "SUCCESS");
            audit(id, actor, "DOWNLOAD", "DOCX", "SUCCESS");
            return file;
        } catch (RuntimeException failure) {
            audit(id, actor, "GENERATE", "DOCX", "FAILED");
            audit(id, actor, "DOWNLOAD", "DOCX", "FAILED");
            throw failure;
        }
    }

    public byte[] exportXlsx(String id) {
        var report = owned(id);
        try {
            byte[] bytes = workbook.generate(statistics(report));
            audit(id, actor(), "GENERATE", "XLSX", "SUCCESS");
            audit(id, actor(), "DOWNLOAD", "XLSX", "SUCCESS");
            return bytes;
        } catch (IOException | RuntimeException failure) {
            audit(id, actor(), "GENERATE", "XLSX", "FAILED");
            audit(id, actor(), "DOWNLOAD", "XLSX", "FAILED");
            throw new IllegalStateException("Unable to generate saved report workbook", failure);
        }
    }

    private PqrsStatisticsSavedReport owned(String id) {
        authorization.requireStatisticalAccess();
        if (id == null || id.isBlank() || id.length() > 64) throw new SavedReportNotFoundException();
        var query = Query.query(Criteria.where("_id").is(id).and("createdBy").is(actor()));
        var report = mongo.findOne(query, PqrsStatisticsSavedReport.class, REPORT_COLLECTION);
        if (report == null) throw new SavedReportNotFoundException();
        return report;
    }

    private PqrsStatisticsDocument document(PqrsStatisticsSavedReport report) {
        var checked = new Checked(
            statistics(report),
            office(report),
            report.getAvailableIndicators(),
            report.getCheckedAt(),
            report.getCheckedAt(),
            report.getModelVersion(),
            report.getMethodologyVersion(),
            report.getCatalogVersion()
        );
        return PqrsStatisticsDocumentAssembler.assemble(
            checked,
            PqrsStatisticsNarrativeText.validate(report.getNarrative()),
            report.getGeneratedAt(),
            UUID.fromString(report.getId())
        );
    }

    private Detail detail(PqrsStatisticsSavedReport report) {
        return new Detail(
            report.getId(),
            report.getTitle(),
            filter(report),
            office(report),
            report.getCutoff(),
            statistics(report),
            report.getNarrative(),
            report.getCreatedBy(),
            report.getModifiedBy(),
            report.getCreatedAt(),
            report.getModifiedAt(),
            report.getGeneratedAt(),
            report.getModelVersion(),
            report.getMethodologyVersion(),
            report.getCatalogVersion(),
            report.getVersion()
        );
    }

    private Summary summary(PqrsStatisticsSavedReport report) {
        return new Summary(
            report.getId(),
            report.getTitle(),
            filter(report),
            office(report),
            report.getCutoff(),
            report.getCreatedAt(),
            report.getModifiedAt(),
            report.getGeneratedAt(),
            report.getCreatedBy(),
            report.getModifiedBy(),
            report.getVersion(),
            report.getReceived()
        );
    }

    private static PqrsStatisticsFilter filter(PqrsStatisticsSavedReport report) {
        return new PqrsStatisticsFilter(
            report.getStartDate(),
            report.getEndDate(),
            PqrsStatisticsFilter.Scope.valueOf(report.getScope()),
            report.getOfficeId()
        );
    }

    private static ResolvedOffice office(PqrsStatisticsSavedReport report) {
        if (report.getOfficeIdentity() == null) return null;
        return new ResolvedOffice(report.getOfficeIdType(), report.getOfficeIdentity(), report.getOfficeName());
    }

    private String serialize(PqrsStatistics statistics) {
        try {
            String snapshot = json.writeValueAsString(statistics);
            if (snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 8 * 1024 * 1024) throw new IllegalStateException(
                "Statistics snapshot exceeds the storage limit"
            );
            return snapshot;
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unable to persist verified statistics", failure);
        }
    }

    private PqrsStatistics statistics(PqrsStatisticsSavedReport report) {
        try {
            return json.readValue(report.getStatisticsSnapshot(), PqrsStatistics.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Unable to read saved statistics snapshot", failure);
        }
    }

    private void audit(String reportId, String actor, String operation, String format, String result) {
        var event = new PqrsStatisticsReportAudit();
        event.setReportId(reportId);
        event.setActor(actor);
        event.setOccurredAt(now());
        event.setOperation(operation);
        event.setFormat(format);
        event.setResult(result);
        mongo.insert(event, AUDIT_COLLECTION);
    }

    private static String actor() {
        return SecurityUtils.getCurrentUserLogin().filter(login -> !login.isBlank()).orElseThrow(SavedReportNotFoundException::new);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MILLIS);
    }
}
