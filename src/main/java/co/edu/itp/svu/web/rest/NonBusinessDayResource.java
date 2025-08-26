package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.domain.NonBusinessDay;
import co.edu.itp.svu.repository.NonBusinessDayRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.service.NonBusinessDayService;
import co.edu.itp.svu.service.PqrsDeadlineAdjustmentService;
import co.edu.itp.svu.service.dto.NonBusinessDayDTO;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link co.edu.itp.svu.domain.NonBusinessDay}.
 */
@RestController
@RequestMapping("/api/admin")
public class NonBusinessDayResource {

    private static final Logger log = LoggerFactory.getLogger(NonBusinessDayResource.class);

    private static final String ENTITY_NAME = "nonBusinessDay";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NonBusinessDayService nonBusinessDayService;

    private final NonBusinessDayRepository nonBusinessDayRepository;

    private final PqrsDeadlineAdjustmentService deadlineAdjustmentService;

    public NonBusinessDayResource(
        NonBusinessDayService nonBusinessDayService,
        NonBusinessDayRepository nonBusinessDayRepository,
        PqrsDeadlineAdjustmentService deadlineAdjustmentService
    ) {
        this.nonBusinessDayService = nonBusinessDayService;
        this.nonBusinessDayRepository = nonBusinessDayRepository;
        this.deadlineAdjustmentService = deadlineAdjustmentService;
    }

    /**
     * {@code POST  /non-business-days} : Create a new nonBusinessDay.
     *
     * @param nonBusinessDayDTO the nonBusinessDayDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new nonBusinessDayDTO, or with status {@code 400 (Bad Request)} if the nonBusinessDay has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/non-business-days")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<NonBusinessDayDTO> createNonBusinessDay(@Valid @RequestBody NonBusinessDayDTO nonBusinessDayDTO)
        throws URISyntaxException {
        log.debug("REST request to save NonBusinessDay : {}", nonBusinessDayDTO);
        if (nonBusinessDayDTO.getId() != null) {
            throw new BadRequestAlertException("A new nonBusinessDay cannot already have an ID", ENTITY_NAME, "idexists");
        }
        log.info("--------- HOW LOOKS THE DATE {}", nonBusinessDayDTO.getDate());
        NonBusinessDayDTO result = nonBusinessDayService.save(nonBusinessDayDTO);
        deadlineAdjustmentService.adjustDeadlinesForDateChange(result.getDate());

        return ResponseEntity.created(new URI("/api/admin/non-business-days/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /non-business-days/:id} : Updates an existing nonBusinessDay.
     *
     * @param id the id of the nonBusinessDayDTO to save.
     * @param nonBusinessDayDTO the nonBusinessDayDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nonBusinessDayDTO.
     */
    @PutMapping("/non-business-days/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<NonBusinessDayDTO> updateNonBusinessDay(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody NonBusinessDayDTO nonBusinessDayDTO
    ) {
        log.debug("REST request to update NonBusinessDay : {}, {}", id, nonBusinessDayDTO);
        if (nonBusinessDayDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nonBusinessDayDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!nonBusinessDayRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        NonBusinessDayDTO result = nonBusinessDayService.update(nonBusinessDayDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, nonBusinessDayDTO.getId()))
            .body(result);
    }

    /**
     * {@code GET  /non-business-days} : get all the nonBusinessDays.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nonBusinessDays in body.
     */
    @GetMapping("/non-business-days")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<List<NonBusinessDayDTO>> getAllNonBusinessDays(@ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of NonBusinessDays");
        Page<NonBusinessDayDTO> page = nonBusinessDayService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /non-business-days/:id} : get the "id" nonBusinessDay.
     *
     * @param id the id of the nonBusinessDayDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the nonBusinessDayDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/non-business-days/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<NonBusinessDayDTO> getNonBusinessDay(@PathVariable String id) {
        log.debug("REST request to get NonBusinessDay : {}", id);
        Optional<NonBusinessDayDTO> nonBusinessDayDTO = nonBusinessDayService.findOne(id);
        return ResponseUtil.wrapOrNotFound(nonBusinessDayDTO);
    }

    /**
     * {@code DELETE  /non-business-days/:id} : delete the "id" nonBusinessDay.
     *
     * @param id the id of the nonBusinessDayDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/non-business-days/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteNonBusinessDay(@PathVariable String id) {
        log.debug("REST request to delete NonBusinessDay : {}", id);
        Optional<NonBusinessDay> nonBusinessDayOptional = nonBusinessDayRepository.findById(id);
        if (nonBusinessDayOptional.isPresent()) {
            NonBusinessDay nonBusinessDay = nonBusinessDayOptional.get();
            nonBusinessDayRepository.deleteById(id);
            deadlineAdjustmentService.adjustDeadlinesForDateChange(nonBusinessDay.getDate());
        }

        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }
}
