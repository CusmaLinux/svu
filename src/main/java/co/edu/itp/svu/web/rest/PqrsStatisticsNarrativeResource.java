package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.report.PqrsStatisticsNarrativeService;
import co.edu.itp.svu.service.report.PqrsStatisticsReceiptException;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.mongodb.MongoException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/gestion-pqrsd")
public class PqrsStatisticsNarrativeResource {

    static final int MAX_BODY = 256 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper(
        JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(4).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build()
    ).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final PqrsStatisticsNarrativeService service;

    public PqrsStatisticsNarrativeResource(PqrsStatisticsNarrativeService service) {
        this.service = service;
    }

    @PostMapping("/narrative-previews")
    public ResponseEntity<PqrsStatisticsNarrativeService.Preview> create(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.preview(readFilter(request)));
    }

    private static PqrsStatisticsFilter readFilter(HttpServletRequest request) {
        try {
            var type = MediaType.parseMediaType(request.getContentType() == null ? "" : request.getContentType());
            if (
                !"application".equalsIgnoreCase(type.getType()) ||
                !"json".equalsIgnoreCase(type.getSubtype()) ||
                (type.getCharset() != null && !StandardCharsets.UTF_8.equals(type.getCharset())) ||
                Collections.list(request.getHeaders(HttpHeaders.CONTENT_ENCODING)).stream().anyMatch(v -> !"identity".equalsIgnoreCase(v))
            ) throw unsupported();
        } catch (IllegalArgumentException e) {
            throw unsupported();
        }
        if (request.getContentLengthLong() > MAX_BODY) throw tooLarge();
        JsonNode root;
        try {
            byte[] data = request.getInputStream().readNBytes(MAX_BODY + 1);
            if (data.length > MAX_BODY) throw tooLarge();
            root = JSON.readTree(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString());
        } catch (IOException e) {
            throw invalid();
        }
        if (root == null || !root.isObject() || root.size() != 1 || !root.path("filter").isObject()) throw invalid();
        var f = root.get("filter");
        var names = f.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (
                !Set.of("start", "end", "scope", "officeId").contains(field) ||
                !f.get(field).isTextual() ||
                f.get(field).textValue().codePoints().anyMatch(c -> c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE)
            ) throw invalid();
        }
        var start = PqrsStatisticsResource.date(f.path("start").textValue());
        var end = PqrsStatisticsResource.date(f.path("end").textValue());
        var scope =
            switch (f.path("scope").asText("")) {
                case "INSTITUTIONAL" -> PqrsStatisticsFilter.Scope.INSTITUTIONAL;
                case "OFFICE" -> PqrsStatisticsFilter.Scope.OFFICE;
                default -> throw new InvalidPqrsStatisticsFilter("invalidScope", "Invalid reporting scope");
            };
        return new PqrsStatisticsFilter(start, end, scope, f.path("officeId").textValue());
    }

    private static PqrsStatisticsReceiptException invalid() {
        return new PqrsStatisticsReceiptException(400, "invalidExportRequest");
    }

    private static PqrsStatisticsReceiptException unsupported() {
        return new PqrsStatisticsReceiptException(415, "unsupportedExportMedia");
    }

    private static PqrsStatisticsReceiptException tooLarge() {
        return new PqrsStatisticsReceiptException(413, "exportBodyTooLarge");
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
