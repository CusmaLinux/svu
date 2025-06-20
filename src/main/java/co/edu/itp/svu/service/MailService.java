package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import tech.jhipster.config.JHipsterProperties;

/**
 * Service for sending emails asynchronously using OAuth2.
 * This service prepares the content from templates and delegates the sending
 * to the OAuth2MailService.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String PQRS = "pqrs";
    private static final String BASE_URL = "baseUrl";

    private final JHipsterProperties jHipsterProperties;
    private final MessageSource messageSource;
    private final SpringTemplateEngine templateEngine;
    private final OAuth2MailService oAuth2MailService;

    public MailService(
        JHipsterProperties jHipsterProperties,
        MessageSource messageSource,
        SpringTemplateEngine templateEngine,
        OAuth2MailService oAuth2MailService
    ) {
        this.jHipsterProperties = jHipsterProperties;
        this.messageSource = messageSource;
        this.templateEngine = templateEngine;
        this.oAuth2MailService = oAuth2MailService;
    }

    /**
     * Asynchronously sends a pre-formatted email.
     *
     * @param to      the recipient's email address.
     * @param subject the subject of the email.
     * @param content the HTML content of the email.
     */
    @Async
    public void sendEmail(String to, String subject, String content) {
        this.sendEmailSync(to, subject, content);
    }

    /**
     * Synchronously sends an email by delegating to the OAuth2MailService.
     * This method is now much simpler.
     *
     * @param to      the recipient's email address.
     * @param subject the subject of the email.
     * @param content the HTML content of the email.
     */
    private void sendEmailSync(String to, String subject, String content) {
        try {
            LOG.debug("Preparing to send email to '{}' with subject '{}' using OAuth2", to, subject);
            oAuth2MailService.sendEmail(to, subject, content);
            LOG.info("Successfully queued email for sending to User '{}' via OAuth2MailService", to);
        } catch (Exception e) {
            LOG.warn("Email could not be sent to user '{}'", to, e);
        }
    }

    private void sendEmailFromTemplateSync(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        this.sendEmailSync(user.getEmail(), subject, content);
    }

    private void sendEmailFromTemplateSync(Pqrs pqrs, String templateName, String titleKey) {
        if (pqrs.getRequesterEmail() == null) {
            LOG.debug("Email doesn't exist for PQRS");
            return;
        }
        Locale locale = Locale.forLanguageTag("es");
        Context context = new Context(locale);
        context.setVariable(PQRS, pqrs);
        context.setVariable(BASE_URL, jHipsterProperties.getMail().getBaseUrl());
        String content = templateEngine.process(templateName, context);
        String subject = messageSource.getMessage(titleKey, null, locale);

        this.sendEmailSync(pqrs.getRequesterEmail(), subject, content);
    }

    @Async
    public void sendActivationEmail(User user) {
        LOG.debug("Sending activation email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/activationEmail", "email.activation.title");
    }

    @Async
    public void sendCreationEmail(User user) {
        LOG.debug("Sending creation email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/creationEmail", "email.activation.title");
    }

    @Async
    public void sendPasswordResetMail(User user) {
        LOG.debug("Sending password reset email to '{}'", user.getEmail());
        this.sendEmailFromTemplateSync(user, "mail/passwordResetEmail", "email.reset.title");
    }

    @Async
    public void sendAccessToken(Pqrs pqrs) {
        LOG.debug("Sending accessToken email to '{}'", pqrs.getRequesterEmail());
        this.sendEmailFromTemplateSync(pqrs, "mail/accessTokenEmail", "email.access_token.title");
    }
}
