package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.service.report.PqrsStatisticsNarrativeService;
import co.edu.itp.svu.service.report.PqrsStatisticsReceiptException;
import co.edu.itp.svu.service.report.PqrsStatisticsWordExport;
import com.mongodb.MongoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/gestion-pqrsd")
public class PqrsStatisticsNarrativeResource {

    private final PqrsStatisticsNarrativeService service;

    private final PqrsStatisticsWordExport word;

    public PqrsStatisticsNarrativeResource(PqrsStatisticsNarrativeService service, PqrsStatisticsWordExport word) {
        this.service = service;
        this.word = word;
    }

    @PostMapping("/export/docx")
    public ResponseEntity<byte[]> docx(HttpServletRequest request) {
        var file = word.generate(() -> PqrsStatisticsNarrativeRequest.export(request));
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
            .header("X-Content-Type-Options", "nosniff")
            .body(file.bytes());
    }

    @PostMapping("/narrative-previews")
    public ResponseEntity<PqrsStatisticsNarrativeService.Preview> create(HttpServletRequest request) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore())
            .body(service.preview(PqrsStatisticsNarrativeRequest.preview(request)));
    }

    @ExceptionHandler(PqrsStatisticsReceiptException.class)
    public ResponseEntity<ProblemDetail> receiptError(PqrsStatisticsReceiptException error) {
        return problem(error.status(), error.getMessage());
    }

    @ExceptionHandler(InvalidPqrsStatisticsFilter.class)
    public ResponseEntity<ProblemDetail> filterError(InvalidPqrsStatisticsFilter error) {
        return problem(400, error.code());
    }

    @ExceptionHandler({ MongoException.class, DataAccessException.class, IllegalStateException.class })
    public ResponseEntity<ProblemDetail> unavailable() {
        return problem(503, "exportUnavailable");
    }

    private static ResponseEntity<ProblemDetail> problem(int status, String code) {
        var body = ProblemDetail.forStatus(status);
        body.setProperty("message", "error." + code);
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
