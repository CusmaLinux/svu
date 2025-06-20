package co.edu.itp.svu.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails using Gmail's SMTP server with OAuth2
 * authentication.
 * <p>
 * This service is designed for server-to-server, background email sending
 * (e.g., in {@code @Async} methods).
 * It uses a long-lived refresh token to automatically obtain new access tokens
 * as needed,
 * avoiding the need for user interaction or storing passwords.
 *
 * @see MailService for the high-level service that prepares email content from
 *      templates.
 */
@Service
public class OAuth2MailService {

    private final Logger LOG = LoggerFactory.getLogger(OAuth2MailService.class);

    private final AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String refreshTokenValue;

    private final String clientRegistrationId;
    private final String user;

    /**
     * Constructs the OAuth2MailService and its required client manager.
     *
     * @param clientRegistrationRepository Repository for client registration
     *                                     details (e.g., client ID, secret).
     * @param authorizedClientService      Service for storing and retrieving
     *                                     authorized clients (containing tokens).
     * @param clientRegistrationId         The ID of the OAuth2 client registration
     *                                     (e.g., "google").
     * @param user                         The principal name (email) of the account
     *                                     being used to send mail.
     * @param refreshTokenValue            The long-lived refresh token obtained
     *                                     from Google's authorization flow.
     */
    public OAuth2MailService(
        ClientRegistrationRepository clientRegistrationRepository,
        OAuth2AuthorizedClientService authorizedClientService,
        @Value("${gmail.oauth2.client-registration-id}") String clientRegistrationId,
        @Value("${gmail.oauth2.user}") String user,
        @Value("${gmail.oauth2.refresh-token}") String refreshTokenValue
    ) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizedClientService = authorizedClientService;
        this.refreshTokenValue = refreshTokenValue;
        this.clientRegistrationId = clientRegistrationId;
        this.user = user;

        this.authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        );

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().refreshToken().build();
        this.authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
    }

    /**
     * Initializes the service by "seeding" the
     * {@link OAuth2AuthorizedClientService} with the refresh token.
     * <p>
     * This method runs once at application startup. It creates an
     * {@link OAuth2AuthorizedClient}
     * with the configured refresh token and a dummy, expired access token. This
     * ensures that the
     * {@link AuthorizedClientServiceOAuth2AuthorizedClientManager} has the
     * necessary information to
     * perform the refresh token grant flow on the first call to {@link #sendEmail}.
     */
    @PostConstruct
    public void seedAuthorizedClient() {
        LOG.info("Seeding OAuth2AuthorizedClient for mail service principal '{}'", user);

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
        if (clientRegistration == null) {
            throw new IllegalStateException("Client registration not found for ID: " + clientRegistrationId);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "dummy-access-token",
            Instant.now().minusSeconds(3600),
            Instant.now().minusSeconds(3599)
        );
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(refreshTokenValue, null);

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(clientRegistration, user, accessToken, refreshToken);

        Authentication principal = new UsernamePasswordAuthenticationToken(this.user, null, null);

        authorizedClientService.saveAuthorizedClient(authorizedClient, principal);
        LOG.info("Successfully seeded OAuth2AuthorizedClient for mail service.");
    }

    /**
     * Sends an HTML email using OAuth2 authentication.
     * <p>
     * This method first obtains a valid OAuth2 access token using the refresh token
     * flow.
     * It then uses this access token as the password to authenticate with Gmail's
     * SMTP server
     * via the XOAUTH2 mechanism.
     *
     * @param to      the recipient's email address.
     * @param subject the subject line of the email.
     * @param content the HTML body of the email.
     * @throws RuntimeException if an access token cannot be obtained or if the
     *                          email fails to send.
     */
    public void sendEmail(String to, String subject, String content) {
        LOG.debug("Request to send email to {} with subject '{}' using OAuth2", to, subject);

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
            .principal(user)
            .build();

        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            LOG.error("Could not get OAuth2 access token for principal '{}'", user);
            throw new RuntimeException("Failed to obtain OAuth2 access token.");
        }

        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        LOG.debug("Successfully obtained OAuth2 access token.");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);
        mailSender.setProtocol("smtp");
        mailSender.setUsername(user);
        mailSender.setPassword(accessToken.getTokenValue());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2");

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(user);
            message.setSubject(subject);
            message.setText(content, true);
            mailSender.send(mimeMessage);
            LOG.info("Sent email to User '{}'", to);
        } catch (MessagingException e) {
            LOG.error("Email could not be sent to user '{}'", to, e);
            throw new RuntimeException("Failed to send email.", e);
        }
    }
}
