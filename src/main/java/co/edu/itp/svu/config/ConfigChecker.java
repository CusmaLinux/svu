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

    @Value("${spring.mail.password:PASSWORD_NOT_FOUND}")
    private String mailPassword;

    @PostConstruct
    public void checkConfig() {
        LOG.info("====================================================================");
        LOG.info("               CHECKING SPRING MAIL CONFIGURATION                   ");
        LOG.info("Mail Username: {}", mailUsername);

        if (mailPassword != null && !mailPassword.equals("PASSWORD_NOT_FOUND")) {
            LOG.info("Mail Password: <is set>");
        } else {
            LOG.info("Mail Password: {}", mailPassword);
        }
        LOG.info("====================================================================");
    }
}
