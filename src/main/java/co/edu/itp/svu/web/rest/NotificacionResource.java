package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.repository.NotificacionRepository;
import co.edu.itp.svu.service.NotificacionService;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link co.edu.itp.svu.domain.Notificacion}.
 */
@RestController
@RequestMapping("/api/notification")
public class NotificacionResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionResource.class);

    private static final String ENTITY_NAME = "notificacion";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NotificacionService notificationService;

    private final NotificacionRepository notificacionRepository;

    public NotificacionResource(NotificacionService notificationService, NotificacionRepository notificacionRepository) {
        this.notificationService = notificationService;
        this.notificacionRepository = notificacionRepository;
    }

    /**
     * {@code POST  /notificacions} : Create a new notificacion.
     *
     * @param notificacionDTO the notificacionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new notificacionDTO, or with status
     *         {@code 400 (Bad Request)} if the notificacion has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<NotificacionDTO> createNotificacion(@Valid @RequestBody NotificacionDTO notificacionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save Notificacion : {}", notificacionDTO);
        if (notificacionDTO.getId() != null) {
            throw new BadRequestAlertException("A new notificacion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        notificacionDTO = notificationService.save(notificacionDTO);
        return ResponseEntity.created(new URI("/api/notification/" + notificacionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, notificacionDTO.getId()))
            .body(notificacionDTO);
    }

    /**
     * {@code PUT  /notificacions/:id} : Updates an existing notificacion.
     *
     * @param id              the id of the notificacionDTO to save.
     * @param notificacionDTO the notificacionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated notificacionDTO,
     *         or with status {@code 400 (Bad Request)} if the notificacionDTO is
     *         not valid,
     *         or with status {@code 500 (Internal Server Error)} if the
     *         notificacionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificacionDTO> updateNotificacion(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody NotificacionDTO notificacionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Notificacion : {}, {}", id, notificacionDTO);
        if (notificacionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notificacionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificacionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        notificacionDTO = notificationService.update(notificacionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificacionDTO.getId()))
            .body(notificacionDTO);
    }

    /**
     * {@code PATCH  /notificacions/:id} : Partial updates given fields of an
     * existing notificacion, field will ignore if it is null
     *
     * @param id              the id of the notificacionDTO to save.
     * @param notificacionDTO the notificacionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated notificacionDTO,
     *         or with status {@code 400 (Bad Request)} if the notificacionDTO is
     *         not valid,
     *         or with status {@code 404 (Not Found)} if the notificacionDTO is not
     *         found,
     *         or with status {@code 500 (Internal Server Error)} if the
     *         notificacionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NotificacionDTO> partialUpdateNotificacion(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody NotificacionDTO notificacionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Notificacion partially : {}, {}", id, notificacionDTO);
        if (notificacionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notificacionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificacionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<NotificacionDTO> result = notificationService.partialUpdate(notificacionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificacionDTO.getId())
        );
    }

    /**
     * {@code GET  /notificacions} : get all the notifications for current user.
     *
     * @param Boolean  the read status of the notification
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     *         of notifications in body.
     */
    @GetMapping("")
    public ResponseEntity<List<NotificacionDTO>> getAllNotifications(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false) Boolean read
    ) {
        LOG.debug("REST request to get a page of Notifications for current user");
        Page<NotificacionDTO> page;

        if (read != null) {
            page = notificationService.findAllForCurrentUserAndReadStatus(read, pageable);
        } else {
            page = notificationService.findAllForCurrentUser(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /notificacions/:id} : get the "id" notificacion.
     *
     * @param id the id of the notificacionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the notificacionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificacionDTO> getNotificacion(@PathVariable("id") String id) {
        LOG.debug("REST request to get Notificacion : {}", id);
        Optional<NotificacionDTO> notificacionDTO = notificationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(notificacionDTO);
    }

    /**
     * {@code DELETE  /notificacions/:id} : delete the "id" notificacion.
     *
     * @param id the id of the notificacionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificacion(@PathVariable("id") String id) {
        LOG.debug("REST request to delete Notificacion : {}", id);
        notificationService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }

    /**
     * {@code PUT /notifications/{id}/mark-as-read} : Marks a specific notification
     * as read.
     *
     * @param id the id of the notificationDTO to mark as read.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} if
     *         successful,
     *         or with status {@code 404 (Not Found)} if the notification does not
     *         exist,
     *         or with status {@code 400 (Bad Request)} if the id is not valid.
     */
    @PutMapping("/{id}/mark-as-read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable String id) {
        LOG.debug("REST request to mark Notification {} as read", id);
        boolean marked = notificationService.markAsRead(id);
        if (marked) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * {@code PUT  /notifications/mark-all-as-read} : Marks all notifications for
     * the current user as read.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/mark-all-as-read")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        LOG.debug("REST request to mark all Notifications as read for current user");
        notificationService.markAllAsReadForCurrentUser();
        return ResponseEntity.ok().build();
    }
}
