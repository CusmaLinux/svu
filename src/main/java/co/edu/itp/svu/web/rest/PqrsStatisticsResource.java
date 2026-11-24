package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import co.edu.itp.svu.service.report.PqrsStatisticsService;
import co.edu.itp.svu.service.report.PqrsStatisticsService.Configuration;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import com.mongodb.MongoException;
import com.mongodb.MongoExecutionTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports/gestion-pqrsd")
public class PqrsStatisticsResource {

    private static final Pattern CIVIL_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private final PqrsStatisticsService statistics;

    public PqrsStatisticsResource(PqrsStatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping("/preview")
    public ResponseEntity<PqrsStatistics> preview(@RequestParam MultiValueMap<String, String> parameters) {
        try {
            LocalDate start = date(parameter(parameters, "start"));
            LocalDate end = date(parameter(parameters, "end"));
            Scope scope =
                switch (Objects.toString(parameter(parameters, "scope"), "")) {
                    case "INSTITUTIONAL" -> Scope.INSTITUTIONAL;
                    case "OFFICE" -> Scope.OFFICE;
                    default -> throw new InvalidPqrsStatisticsFilter("invalidScope", "Select a valid reporting scope");
                };
            var filter = new PqrsStatisticsFilter(start, end, scope, parameter(parameters, "officeId"));
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(statistics.calculate(filter));
        } catch (InvalidPqrsStatisticsFilter exception) {
            throw new BadRequestAlertException(exception.getMessage(), "pqrsStatistics", exception.code());
        }
    }

    @GetMapping("/configuration")
    public ResponseEntity<Configuration> configuration() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(statistics.configuration());
    }

    private static String parameter(MultiValueMap<String, String> parameters, String name) {
        List<String> values = parameters.get(name);
        if (values == null) return null;
        if (values.size() != 1) throw new InvalidPqrsStatisticsFilter("repeatedParameter", "Repeat-free reporting parameters required");
        return values.get(0);
    }

    private static LocalDate date(String value) {
        if (value != null && CIVIL_DATE.matcher(value).matches()) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ignored) {
                // Keep parser internals and the received value out of the problem response.
            }
        }
        throw new InvalidPqrsStatisticsFilter("invalidDate", "Start and end must be civil dates in YYYY-MM-DD format");
    }

    @ExceptionHandler({ MongoExecutionTimeoutException.class, QueryTimeoutException.class })
    public ResponseEntity<ProblemDetail> timeout() {
        return unavailable("The statistics query exceeded its time limit");
    }

    @ExceptionHandler({ MongoException.class, DataAccessException.class })
    public ResponseEntity<ProblemDetail> dataUnavailable() {
        return unavailable("Statistics are temporarily unavailable");
    }

    private static ResponseEntity<ProblemDetail> unavailable(String detail) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .cacheControl(CacheControl.noStore())
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, detail));
    }
}
