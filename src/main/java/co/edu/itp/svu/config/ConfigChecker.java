package co.edu.itp.svu.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigChecker {

    private final Logger LOG = LoggerFactory.getLogger(ConfigChecker.class);

    @Value("${spring.mail.username:USERNAME_NOT_FOUND}")
    private String mailUsername;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:CLIENT_SECRET_NOT_FOUND}")
    private String clientSecret;

    @PostConstruct
    public void checkConfig() {
        LOG.info("====================================================================");
        LOG.info("               CHECKING SPRING MAIL OAUTH2.0 CONFIGURATION                   ");
        LOG.info("Mail Username: {}", mailUsername);
        if (clientSecret != null && !clientSecret.equals("CLIENT_SECRET_NOT_FOUND")) {
            LOG.info("Client Secret: <is set>");
        } else {
            LOG.info("Client Secret: {}", clientSecret);
        }
        LOG.info("====================================================================");
    }
}
