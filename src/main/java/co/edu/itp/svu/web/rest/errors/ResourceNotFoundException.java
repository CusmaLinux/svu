package co.edu.itp.svu.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;
import tech.jhipster.web.rest.errors.ProblemDetailWithCause.ProblemDetailWithCauseBuilder;

public class ResourceNotFoundException extends ErrorResponseException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(
            HttpStatus.NOT_FOUND,
            ProblemDetailWithCauseBuilder.instance()
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withTitle("Resource Not Found")
                .withProperty("message", message)
                .build(),
            null
        );
    }
}
