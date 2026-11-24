package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.notification.SseNotificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse-notifications")
public class SseNotificationResource {

    private final PqrsAuthorizationService authorization;
    private final UserRepository userRepository;
    private final SseNotificationService sseNotificationService;

    public SseNotificationResource(
        SseNotificationService sseNotificationService,
        UserRepository userRepository,
        PqrsAuthorizationService authorization
    ) {
        this.authorization = authorization;
        this.userRepository = userRepository;
        this.sseNotificationService = sseNotificationService;
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribeToNotifications() {
        authorization.requireOperationalOffice();
        User recipient = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .orElseThrow(() -> new AccessDeniedException("Unknown SSE subscriber"));
        SseEmitter emitter = sseNotificationService.createEmitter(recipient.getId());
        return ResponseEntity.ok(emitter);
    }
}
