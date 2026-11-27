package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.report.PqrsStatisticsReceiptException;
import co.edu.itp.svu.service.report.PqrsStatisticsSavedReportService;
import co.edu.itp.svu.service.report.SavedReportConflictException;
import co.edu.itp.svu.service.report.SavedReportNotFoundException;
import com.mongodb.MongoException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/gestion-pqrsd/saved")
public class PqrsStatisticsSavedReportResource {

    private final PqrsStatisticsSavedReportService reports;

    public PqrsStatisticsSavedReportResource(PqrsStatisticsSavedReportService reports) {
        this.reports = reports;
    }

    @PostMapping
    public ResponseEntity<PqrsStatisticsSavedReportService.Detail> create(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .cacheControl(CacheControl.noStore())
            .body(reports.create(PqrsStatisticsNarrativeRequest.export(request)));
    }

    @GetMapping
    public ResponseEntity<PqrsStatisticsSavedReportService.Page> list(
        @RequestParam(required = false) String scope,
        @RequestParam(required = false) String start,
        @RequestParam(required = false) String end,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        PqrsStatisticsFilter.Scope parsedScope = null;
        if (scope != null) {
            parsedScope = switch (scope) {
                case "INSTITUTIONAL" -> PqrsStatisticsFilter.Scope.INSTITUTIONAL;
                case "OFFICE" -> PqrsStatisticsFilter.Scope.OFFICE;
                default -> throw new InvalidPqrsStatisticsFilter("invalidScope", "Invalid reporting scope");
            };
        }
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(reports.list(parsedScope, parseDate(start), parseDate(end), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PqrsStatisticsSavedReportService.Detail> get(@PathVariable String id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.get(id));
    }

    @PutMapping("/{id}/narrative")
    public ResponseEntity<PqrsStatisticsSavedReportService.Detail> updateNarrative(@PathVariable String id, HttpServletRequest request) {
        var update = PqrsStatisticsNarrativeRequest.update(request);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(reports.updateNarrative(id, update.expectedVersion(), update.narrative()));
    }

    @GetMapping("/{id}/export/docx")
    public ResponseEntity<byte[]> docx(@PathVariable String id) {
        var file = reports.exportWord(id);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(file.bytes());
    }

    @GetMapping("/{id}/export/xlsx")
    public ResponseEntity<byte[]> xlsx(@PathVariable String id) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename("Informe_PQRSD_" + id + ".xlsx").build().toString()
            )
            .header("X-Content-Type-Options", "nosniff")
            .body(reports.exportXlsx(id));
    }

    @ExceptionHandler(SavedReportNotFoundException.class)
    public ResponseEntity<ProblemDetail> notFound() {
        return problem(HttpStatus.NOT_FOUND, "savedReportNotFound");
    }

    @ExceptionHandler(SavedReportConflictException.class)
    public ResponseEntity<ProblemDetail> conflict() {
        return problem(HttpStatus.CONFLICT, "savedReportConflict");
    }

    @ExceptionHandler(PqrsStatisticsReceiptException.class)
    public ResponseEntity<ProblemDetail> receiptError(PqrsStatisticsReceiptException error) {
        return problem(HttpStatus.valueOf(error.status()), error.getMessage());
    }

    @ExceptionHandler({ InvalidPqrsStatisticsFilter.class, IllegalArgumentException.class })
    public ResponseEntity<ProblemDetail> invalidRequest() {
        return problem(HttpStatus.BAD_REQUEST, "invalidSavedReportRequest");
    }

    @ExceptionHandler({ MongoException.class, DataAccessException.class, IllegalStateException.class })
    public ResponseEntity<ProblemDetail> unavailable() {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "savedReportUnavailable");
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException invalid) {
            throw new InvalidPqrsStatisticsFilter("invalidDate", "Invalid reporting date");
        }
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code) {
        var body = ProblemDetail.forStatus(status);
        body.setProperty("message", "error." + code);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
