package co.edu.itp.svu.service.notification;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.repository.UserRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
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
        PQRS_CREATED("PQRS_CREATED", "Se ha radicado una PQRSD."),
        PQRS_RESOLVED("PQRS_STATE_UPDATE", "Se ha resuelto una PQRSD."),
        PQRS_ASSIGNED("PQRS_ASSIGNED", "Se ha asignado una PQRSD para revisión."),
        PQRS_DUE_DATE_REMINDER("PQRS_DUE_DATE_REMINDER", "Hay una PQRSD próxima a vencer.");

        private final String key;
        private final String message;

        PqrsNotificationType(String key, String message) {
            this.key = key;
            this.message = message;
        }

        public String message() {
            return message;
        }

        public static Optional<PqrsNotificationType> fromKey(String key) {
            return Arrays.stream(values()).filter(type -> type.key.equals(key)).findFirst();
        }
    }

    public void sendPqrsNotification(Pqrs pqrs, PqrsNotificationType type, User recipientUser) {
        if (recipientUser == null) {
            LOG.warn("Notification has no recipient");
            return;
        }

        Notificacion persistentNotification = new Notificacion();
        persistentNotification.setFecha(Instant.now());
        persistentNotification.setLeido(false);
        persistentNotification.setMensaje(type.message + " (ID: " + pqrs.getId() + ")");
        persistentNotification.setRecipient(recipientUser);
        persistentNotification.setTipo(type.key);
        notificacionRepository.save(persistentNotification);

        Map<String, Object> event = Map.of(
            "id",
            persistentNotification.getId(),
            "type",
            type.key,
            "message",
            type.message,
            "creationDate",
            persistentNotification.getFecha().toString()
        );
        sseNotificationService.sendNotificationToUser(recipientUser.getId(), type.key, event);
    }

    public void sendPqrsNotification(Pqrs pqrs, PqrsNotificationType type, String recipientAuthority) {
        User recipient = userRepository.findByAuthoritiesName(recipientAuthority);
        if (recipient == null) {
            LOG.warn("Notification role has no recipient");
            return;
        }
        sendPqrsNotification(pqrs, type, recipient);
    }
}
