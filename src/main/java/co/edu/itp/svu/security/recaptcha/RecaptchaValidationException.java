package co.edu.itp.svu.security.recaptcha;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "reCAPTCHA validation failed")
public class RecaptchaValidationException extends RuntimeException {

    public RecaptchaValidationException(String message) {
        super(message);
    }
}
