package co.edu.itp.svu.service;

import co.edu.itp.svu.web.rest.vm.RecaptchaResponseVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaService {

    private final Logger LOG = LoggerFactory.getLogger(RecaptchaService.class);
    private final RestTemplate restTemplate;

    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    @Value("${google.recaptcha.verify-url}")
    private String verifyUrl;

    @Value("${google.recaptcha.score-threshold}")
    private float scoreThreshold;

    public RecaptchaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean verify(String token) {
        if (token == null || token.isEmpty()) {
            LOG.warn("reCAPTCHA token is empty or null");
            return false;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey);
        body.add("response", token);

        try {
            RecaptchaResponseVM response = restTemplate.postForObject(verifyUrl, body, RecaptchaResponseVM.class);

            if (response == null) {
                LOG.error("reCAPTCHA verification response was null.");
                return false;
            }

            LOG.debug("reCAPTCHA verification response: success={}, score={}", response.isSuccess(), response.getScore());

            if (!response.isSuccess()) {
                LOG.warn("reCAPTCHA verification failed with error codes: {}", response.getErrorCodes());
                return false;
            }

            return response.getScore() >= scoreThreshold;
        } catch (Exception e) {
            LOG.error("Error during reCAPTCHA verification request: {}", e.getMessage());
            return false;
        }
    }
}
