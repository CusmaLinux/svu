package co.edu.itp.svu.service.notification;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PqrsNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(PqrsNotificationService.class);

    private final NotificacionRepository notificacionRepository;
    private final UserRepository userRepository;
    private final SseNotificationService sseNotificationService;

    public PqrsNotificationService(
        NotificacionRepository notificacionRepository,
        UserRepository userRepository,
        SseNotificationService sseNotificationService
    ) {
        this.notificacionRepository = notificacionRepository;
        this.userRepository = userRepository;
        this.sseNotificationService = sseNotificationService;
    }

    public enum PqrsNotificationType {
        PQRS_CREATED("PQRS '%s' (ID: %s) was CREATED", "PQRS_CREATED") {
            @Override
            public String getFormattedMessage(Pqrs pqrs, Object... additionalArgs) {
                return String.format(this.messageTemplate, pqrs.getTitulo(), pqrs.getId());
            }

            @Override
            public Map<String, Object> buildSseData(Pqrs pqrs, User recipient, String message, String persistentNotificationId) {
                return Map.of(
                    "id",
                    UUID.randomUUID().toString(),
                    "type",
                    getEventKey(),
                    "creationDate",
                    LocalDate.now().toString(),
                    "message",
                    message,
                    "read",
                    false,
                    "userLogin",
                    recipient.getLogin(),
                    "pqrsId",
                    pqrs.getId(),
                    "pqrsTitle",
                    pqrs.getTitulo(),
                    "isSse",
                    true,
                    "persistentNotificationId",
                    persistentNotificationId
                );
            }
        },
        PQRS_RESOLVED("PQRS '%s' (ID: %s) status was updated to RESOLVED", "PQRS_STATE_UPDATE") {
            @Override
            public String getFormattedMessage(Pqrs pqrs, Object... additionalArgs) {
                return String.format(this.messageTemplate, pqrs.getTitulo(), pqrs.getId());
            }

            @Override
            public Map<String, Object> buildSseData(Pqrs pqrs, User recipient, String message, String persistentNotificationId) {
                return Map.of(
                    "id",
                    UUID.randomUUID().toString(),
                    "type",
                    getEventKey(),
                    "creationDate",
                    LocalDate.now().toString(),
                    "message",
                    message,
                    "read",
                    false,
                    "userLogin",
                    recipient.getLogin(),
                    "pqrsId",
                    pqrs.getId(),
                    "pqrsTitle",
                    pqrs.getTitulo(),
                    "isSse",
                    true,
                    "persistentNotificationId",
                    persistentNotificationId
                );
            }
        },
        PQRS_DUE_DATE_REMINDER("PQRS '%s' (ID: %s) is due on %s.", "PQRS_DUE_DATE_REMINDER") {
            @Override
            public String getFormattedMessage(Pqrs pqrs, Object... additionalArgs) {
                return String.format(this.messageTemplate, pqrs.getTitulo(), pqrs.getId(), pqrs.getFechaLimiteRespuesta().toString());
            }

            @Override
            public Map<String, Object> buildSseData(Pqrs pqrs, User recipient, String message, String persistentNotificationId) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", UUID.randomUUID().toString());
                data.put("type", getEventKey());
                data.put("creationDate", LocalDate.now().toString());
                data.put("message", message);
                data.put("read", false);
                data.put("userLogin", recipient.getLogin());
                data.put("pqrsId", pqrs.getId());
                data.put("pqrsTitle", pqrs.getTitulo());
                data.put("isSse", true);
                data.put("persistentNotificationId", persistentNotificationId);

                if (pqrs.getFechaLimiteRespuesta() != null) {
                    data.put("pqrsResponseDueDate", pqrs.getFechaLimiteRespuesta());
                }
                return data;
            }
        };

        protected final String messageTemplate;
        protected final String eventKey;

        PqrsNotificationType(String messageTemplate, String eventKey) {
            this.messageTemplate = messageTemplate;
            this.eventKey = eventKey;
        }

        public String getEventKey() {
            return eventKey;
        }

        /**
         * Gets the formatted message for this notification type.
         *
         * @param pqrs           The Pqrs entity.
         * @param additionalArgs Optional additional arguments for formatting, if the
         *                       template needs them beyond pqrs fields.
         * @return The formatted message string.
         */
        public abstract String getFormattedMessage(Pqrs pqrs, Object... additionalArgs);

        /**
         * Builds the data map for an SSE notification.
         *
         * @param pqrs                     The Pqrs entity.
         * @param recipient                The recipient user.
         * @param message                  The pre-formatted message string.
         * @param persistentNotificationId The ID of the saved persistent notification.
         * @return A map containing data for the SSE event.
         */
        public abstract Map<String, Object> buildSseData(Pqrs pqrs, User recipient, String message, String persistentNotificationId);
    }

    public void sendPqrsNotification(Pqrs pqrs, PqrsNotificationType type, User recipientUser, Object... messageArgs) {
        if (recipientUser == null) {
            LOG.warn("Recipient user is null for PQRS ID: {}. Notification not sent.", pqrs.getId());
            return;
        }

        String message = type.getFormattedMessage(pqrs, messageArgs);

        Notificacion persistentNotification = new Notificacion();
        persistentNotification.setFecha(Instant.now());
        persistentNotification.setLeido(false);
        persistentNotification.setMensaje(message);
        persistentNotification.setRecipient(recipientUser);
        persistentNotification.setTipo(type.getEventKey());
        notificacionRepository.save(persistentNotification);

        Map<String, Object> sseData = type.buildSseData(pqrs, recipientUser, message, persistentNotification.getId().toString());

        sseNotificationService.sendNotificationToUser(recipientUser.getLogin(), type.getEventKey(), sseData);
    }

    public void sendPqrsNotification(Pqrs pqrs, PqrsNotificationType type, String recipientAuthority, Object... messageArgs) {
        User recipient = userRepository.findByAuthoritiesName(recipientAuthority);
        if (recipient == null) {
            LOG.warn(
                "No user found with authority {} to send notification for PQRS ID: {}. Notification not sent.",
                recipientAuthority,
                pqrs.getId()
            );
            return;
        }
        sendPqrsNotification(pqrs, type, recipient, messageArgs);
    }
}
