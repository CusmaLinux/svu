package co.edu.itp.svu.web.rest.errors;

public class ResourceNotFoundException extends co.edu.itp.svu.service.errors.ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
