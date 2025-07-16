package co.edu.itp.svu.security.recaptcha;

import co.edu.itp.svu.service.RecaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class RecaptchaAspect {

    private final Logger LOG = LoggerFactory.getLogger(RecaptchaAspect.class);
    private static final String RECAPTCHA_HEADER = "X-Recaptcha-Token";
    private final RecaptchaService recaptchaService;

    public RecaptchaAspect(RecaptchaService recaptchaService) {
        this.recaptchaService = recaptchaService;
    }

    @Before("@annotation(co.edu.itp.svu.security.recaptcha.ValidateRecaptcha)")
    public void validateRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String token = request.getHeader(RECAPTCHA_HEADER);

        if (!recaptchaService.verify(token)) {
            LOG.warn("Recaptcha validation failed for request: {}", request.getRequestURI());
            throw new RecaptchaValidationException("reCAPTCHA validation failed.");
        }
        LOG.debug("Recaptcha validation successful.");
    }
}
