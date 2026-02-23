package co.edu.itp.svu.service.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

public class ConflictException extends ErrorResponseException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(
            HttpStatus.CONFLICT,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.CONFLICT.value())
                .withTitle("Conflict")
                .withProperty("message", message)
                .build(),
            null
        );
    }
}
