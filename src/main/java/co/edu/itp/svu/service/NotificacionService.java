package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import co.edu.itp.svu.service.mapper.NotificacionMapper;
import com.mongodb.client.result.UpdateResult;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing
 * {@link co.edu.itp.svu.domain.Notificacion}.
 */
@Service
public class NotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;

    private final NotificacionMapper notificacionMapper;

    private final UserRepository userRepository;

    private final MongoTemplate mongoTemplate;

    public NotificacionService(
        NotificacionRepository notificacionRepository,
        NotificacionMapper notificacionMapper,
        UserRepository userRepository,
        MongoTemplate mongoTemplate
    ) {
        this.notificacionRepository = notificacionRepository;
        this.notificacionMapper = notificacionMapper;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Save a notificacion.
     *
     * @param notificacionDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificacionDTO save(NotificacionDTO notificacionDTO) {
        LOG.debug("Request to save Notificacion : {}", notificacionDTO);
        Notificacion notificacion = notificacionMapper.toEntity(notificacionDTO);
        notificacion = notificacionRepository.save(notificacion);
        return notificacionMapper.toDto(notificacion);
    }

    /**
     * Update a notificacion.
     *
     * @param notificacionDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificacionDTO update(NotificacionDTO notificacionDTO) {
        LOG.debug("Request to update Notificacion : {}", notificacionDTO);
        Notificacion notificacion = notificacionMapper.toEntity(notificacionDTO);
        notificacion = notificacionRepository.save(notificacion);
        return notificacionMapper.toDto(notificacion);
    }

    /**
     * Partially update a notificacion.
     *
     * @param notificacionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<NotificacionDTO> partialUpdate(NotificacionDTO notificacionDTO) {
        LOG.debug("Request to partially update Notificacion : {}", notificacionDTO);

        return notificacionRepository
            .findById(notificacionDTO.getId())
            .map(existingNotificacion -> {
                notificacionMapper.partialUpdate(existingNotificacion, notificacionDTO);

                return existingNotificacion;
            })
            .map(notificacionRepository::save)
            .map(notificacionMapper::toDto);
    }

    /**
     * Get all the notificacions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<NotificacionDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Notificacions");
        return notificacionRepository.findAll(pageable).map(notificacionMapper::toDto);
    }

    /**
     * Get all the notificacions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<NotificacionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return notificacionRepository.findAllWithEagerRelationships(pageable).map(notificacionMapper::toDto);
    }

    /**
     * Get all notifications for the currently authenticated user.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     * @throws RuntimeException if the current user's login cannot be found.
     */
    @Transactional(readOnly = true)
    public Page<NotificacionDTO> findAllForCurrentUser(Pageable pageable) {
        LOG.debug("Request to get all Notifications for current user");

        Optional<String> currentUserLoginOpt = SecurityUtils.getCurrentUserLogin();

        if (currentUserLoginOpt.isEmpty()) {
            LOG.error("Current user login not found in security context");
            return Page.empty(pageable);
        }

        String currentUserLogin = currentUserLoginOpt.get();

        Optional<User> userOpt = userRepository.findOneByLogin(currentUserLogin);
        if (userOpt.isEmpty()) {
            LOG.warn("User with login {} not found, cannot fetch notifications.", currentUserLogin);
            return Page.empty(pageable);
        }

        String recipientId = userOpt.get().getId();

        return notificacionRepository.findAllByRecipientId(recipientId, pageable).map(notificacionMapper::toDto);
    }

    /**
     * Get one notificacion by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<NotificacionDTO> findOne(String id) {
        LOG.debug("Request to get Notificacion : {}", id);
        return notificacionRepository.findOneWithEagerRelationships(id).map(notificacionMapper::toDto);
    }

    /**
     * Delete the notificacion by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete Notificacion : {}", id);
        notificacionRepository.deleteById(id);
    }

    /**
     * Get all notifications for the currently authenticated user and read status.
     *
     * @param Boolean  the read status of the notification
     * @param pageable the pagination information.
     * @return the list of entities.
     * @throws RuntimeException if the current user's login cannot be found.
     */
    @Transactional(readOnly = true)
    public Page<NotificacionDTO> findAllForCurrentUserAndReadStatus(Boolean read, Pageable pageable) {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("Current user login not found"));

        User currentUser = userRepository
            .findOneByLogin(currentUserLogin)
            .orElseThrow(() -> new IllegalStateException("User not found: " + currentUserLogin));

        return notificacionRepository
            .findAllByRecipientIdAndLeidoOrderByFechaDesc(currentUser.getId(), read, pageable)
            .map(notificacionMapper::toDto);
    }

    public boolean markAsRead(String id) {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin().orElse(null);
        if (currentUserLogin == null) {
            throw new IllegalStateException("Current user login not found for marking notification as read.");
        }

        Optional<Notificacion> notificationOpt = notificacionRepository.findById(id);
        if (notificationOpt.isPresent()) {
            Notificacion notificacion = notificationOpt.get();
            if (notificacion.getRecipient() != null && !notificacion.getRecipient().getLogin().equals(currentUserLogin)) {
                LOG.warn("User {} attempted to mark notification {} of another user as read.", currentUserLogin, id);
                return false;
            }

            if (!Boolean.TRUE.equals(notificacion.getLeido())) {
                notificacion.setLeido(true);
                notificacionRepository.save(notificacion);
                return true;
            }
            return true;
        }
        return false;
    }

    public void markAllAsReadForCurrentUser() {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("Current user login not found for marking all notifications as read."));

        User currentUser = userRepository
            .findOneByLogin(currentUserLogin)
            .orElseThrow(() -> new IllegalStateException("User not found: " + currentUserLogin + " for marking all notifications as read.")
            );

        Query query = new Query();
        query.addCriteria(Criteria.where("recipient").is(currentUser));
        query.addCriteria(Criteria.where("leido").is(false));

        Update update = new Update();
        update.set("leido", true);

        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, Notificacion.class);

        LOG.debug("Marked all as read for user {}. Notifications updated: {}", currentUserLogin, updateResult.getModifiedCount());
    }
}
