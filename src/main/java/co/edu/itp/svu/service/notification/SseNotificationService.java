package co.edu.itp.svu.service.notification;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service implementation for managing the SSE emitters(connections)
 */
@Service
public class SseNotificationService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long EMITTER_TIMEOUT = 30 * 60 * 1000L;

    public SseEmitter createEmitter(String recipientId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitter.onCompletion(() -> emitters.remove(recipientId, emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitters.remove(recipientId, emitter));
        SseEmitter previous = emitters.put(recipientId, emitter);
        if (previous != null) previous.complete();
        try {
            emitter.send(SseEmitter.event().name("connected").data("connected"));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(recipientId, emitter);
            emitter.completeWithError(e);
        }
        return emitter;
    }

    public void sendNotificationToUser(String recipientId, String eventName, Object data) {
        SseEmitter emitter = this.emitters.get(recipientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(recipientId, emitter);
                emitter.completeWithError(e);
            }
        }
    }
}
