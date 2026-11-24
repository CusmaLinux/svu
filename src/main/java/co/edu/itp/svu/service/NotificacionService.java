package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import co.edu.itp.svu.service.mapper.NotificacionMapper;
import co.edu.itp.svu.service.notification.PqrsNotificationService.PqrsNotificationType;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {

    // Both historical and current system messages carry the server's case reference after the title.
    private static final Pattern CASE_REFERENCE = Pattern.compile("\\(ID: ([a-zA-Z0-9_-]+)\\)");
    private final NotificacionRepository notices;
    private final UserRepository users;
    private final PqrsRepository cases;
    private final PqrsAuthorizationService authorization;
    private final NotificacionMapper mapper;
    private final MongoTemplate mongo;

    public NotificacionService(
        NotificacionRepository notices,
        UserRepository users,
        PqrsRepository cases,
        PqrsAuthorizationService authorization,
        NotificacionMapper mapper,
        MongoTemplate mongo
    ) {
        this.notices = notices;
        this.users = users;
        this.cases = cases;
        this.authorization = authorization;
        this.mapper = mapper;
        this.mongo = mongo;
    }

    private User currentRecipient() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(users::findOneByLogin)
            .orElseThrow(() -> new AccessDeniedException("Unknown notification recipient"));
    }

    private Query owned(String id) {
        return Query.query(Criteria.where("recipient").is(currentRecipient()).and("id").is(id));
    }

    private NotificacionDTO toInboxDto(Notificacion notice) {
        NotificacionDTO dto = mapper.toDto(notice);
        dto.setRecipientDto(null);

        Optional<PqrsNotificationType> type = PqrsNotificationType.fromKey(notice.getTipo());
        if (type.isPresent()) {
            dto.setMensaje(type.orElseThrow().message());
        } else if (!SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            // Historical free text and custom types may contain case details.
            dto.setTipo("NOTIFICATION");
            dto.setMensaje("Tiene una notificación.");
        }
        return dto;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public NotificacionDTO save(NotificacionDTO input) {
        Notificacion notice = mapper.toEntity(input);
        notice.setRecipient(currentRecipient());
        return toInboxDto(notices.insert(notice));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Optional<NotificacionDTO> edit(String id, NotificacionDTO input, boolean partial) {
        Update change = new Update();
        if (!partial || input.getTipo() != null) change.set("tipo", input.getTipo());
        if (!partial || input.getFecha() != null) change.set("fecha", input.getFecha());
        if (!partial || input.getMensaje() != null) change.set("mensaje", input.getMensaje());
        if (!partial || input.getLeido() != null) change.set("leido", input.getLeido());
        if (change.getUpdateObject().isEmpty()) return findOne(id);
        return Optional.ofNullable(
            mongo.findAndModify(owned(id), change, FindAndModifyOptions.options().returnNew(true), Notificacion.class)
        ).map(this::toInboxDto);
    }

    public Page<NotificacionDTO> findAllForCurrentUser(Pageable pageable) {
        return notices.findAllByRecipientId(currentRecipient().getId(), pageable).map(this::toInboxDto);
    }

    public Page<NotificacionDTO> findAllForCurrentUserAndReadStatus(Boolean read, Pageable pageable) {
        return notices.findAllByRecipientIdAndLeidoOrderByFechaDesc(currentRecipient().getId(), read, pageable).map(this::toInboxDto);
    }

    public Optional<NotificacionDTO> findOne(String id) {
        return Optional.ofNullable(mongo.findOne(owned(id), Notificacion.class)).map(this::toInboxDto);
    }

    public Optional<String> findCaseId(String id) {
        Notificacion notice = mongo.findOne(owned(id), Notificacion.class);
        if (notice == null || notice.getMensaje() == null || PqrsNotificationType.fromKey(notice.getTipo()).isEmpty()) {
            return Optional.empty();
        }
        Matcher refs = CASE_REFERENCE.matcher(notice.getMensaje());
        String caseId = null;
        while (refs.find()) caseId = refs.group(1);
        return Optional.ofNullable(caseId)
            .flatMap(cases::findById)
            .map(persisted -> {
                authorization.checkCaseReadAccess(persisted);
                return persisted.getId();
            });
    }

    public boolean delete(String id) {
        return mongo.remove(owned(id), Notificacion.class).getDeletedCount() == 1;
    }

    public boolean markAsRead(String id) {
        return mongo.updateFirst(owned(id), Update.update("leido", true), Notificacion.class).getMatchedCount() == 1;
    }

    public void markAllAsReadForCurrentUser() {
        Query unread = Query.query(Criteria.where("recipient").is(currentRecipient()).and("leido").is(false));
        mongo.updateMulti(unread, Update.update("leido", true), Notificacion.class);
    }
}
