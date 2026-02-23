package co.edu.itp.svu.web.rest.errors;

public class ConflictException extends co.edu.itp.svu.service.errors.ConflictException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
