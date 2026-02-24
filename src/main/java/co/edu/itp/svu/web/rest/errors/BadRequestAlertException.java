package co.edu.itp.svu.web.rest.errors;

import java.net.URI;

public class BadRequestAlertException extends co.edu.itp.svu.service.errors.BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public BadRequestAlertException(String defaultMessage, String entityName, String errorKey) {
        super(defaultMessage, entityName, errorKey);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey) {
        super(type, defaultMessage, entityName, errorKey);
    }
}
