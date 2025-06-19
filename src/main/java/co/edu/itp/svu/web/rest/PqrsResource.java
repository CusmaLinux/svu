package co.edu.itp.svu.web.rest;

import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.service.PqrsService;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicPqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicResponseDTO;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link co.edu.itp.svu.domain.Pqrs}.
 */
@RestController
@RequestMapping("/api")
public class PqrsResource {

    private static final Logger LOG = LoggerFactory.getLogger(PqrsResource.class);

    private static final String ENTITY_NAME = "pqrs";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PqrsService pqrsService;

    private final PqrsRepository pqrsRepository;

    public PqrsResource(PqrsService pqrsService, PqrsRepository pqrsRepository) {
        this.pqrsService = pqrsService;
        this.pqrsRepository = pqrsRepository;
    }

    /**
     * {@code PATCH  /pqrs/:id} : Partial updates given fields of an existing pqrs,
     * field will ignore if it is null
     *
     * @param id      the id of the pqrsDTO to save.
     * @param pqrsDTO the pqrsDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated pqrsDTO,
     *         or with status {@code 400 (Bad Request)} if the pqrsDTO is not valid,
     *         or with status {@code 404 (Not Found)} if the pqrsDTO is not found,
     *         or with status {@code 500 (Internal Server Error)} if the pqrsDTO
     *         couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/pqrs/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PqrsDTO> partialUpdatePqrs(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody PqrsDTO pqrsDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Pqrs partially : {}, {}", id, pqrsDTO);
        if (pqrsDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, pqrsDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!pqrsRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PqrsDTO> result = pqrsService.partialUpdate(pqrsDTO);

        return ResponseUtil.wrapOrNotFound(result, HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, pqrsDTO.getId()));
    }

    /**
     * {@code GET  /pqrs} : get all the pqrs.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     *         of pqrs in body.
     */
    @GetMapping("/pqrs")
    public ResponseEntity<List<PqrsDTO>> getAllPqrs(
        @RequestParam(defaultValue = "CERRADA", required = false) String state,
        @RequestParam(required = false) String idOffice,
        @RequestParam(name = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of Pqrs");
        if (date == null) {
            date = LocalDate.now();
        }
        LocalDate deadline = date.plusDays(1);
        Page<PqrsDTO> page = pqrsService.findAll(state, idOffice, deadline, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /pqrs/:id} : get the "id" pqrs.
     *
     * @param id the id of the pqrsDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the pqrsDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/pqrs/{id}")
    public ResponseEntity<PqrsDTO> getPqrs(@PathVariable("id") String id) {
        LOG.debug("REST request to get Pqrs : {}", id);
        Optional<PqrsDTO> pqrsDTO = pqrsService.findOneOficina(id);
        return ResponseUtil.wrapOrNotFound(pqrsDTO);
    }

    /**
     * {@code DELETE  /pqrs/:id} : delete the "id" pqrs.
     *
     * @param id the id of the pqrsDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/pqrs/{id}")
    public ResponseEntity<Void> deletePqrs(@PathVariable("id") String id) {
        LOG.debug("REST request to delete Pqrs : {}", id);
        pqrsService.delete(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id)).build();
    }

    /**
     * {@code POST  /pqrs} : Create a new pqrs.
     *
     * @param pqrsDTO the pqrsDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new pqrsDTO, or with status {@code 400 (Bad Request)} if the
     *         pqrs has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/pqrs")
    public ResponseEntity<PqrsDTO> createPqrs(@RequestBody PqrsDTO pqrsDTO) throws IOException {
        LOG.debug("REST request to save Pqrs: {}", pqrsDTO);
        PqrsDTO result = pqrsService.create(pqrsDTO);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code PUT  /pqrs/:id} : Updates an existing pqrs.
     *
     * @param id      the id of the pqrsDTO to save.
     * @param pqrsDTO the pqrsDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the updated pqrsDTO,
     *         or with status {@code 400 (Bad Request)} if the pqrsDTO is not valid,
     *         or with status {@code 500 (Internal Server Error)} if the pqrsDTO
     *         couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/pqrs/{id}")
    public ResponseEntity<PqrsDTO> updatePqrs(@PathVariable String id, @RequestBody PqrsDTO pqrsDTO) {
        LOG.debug("REST request to update Pqrs: {}", pqrsDTO);
        pqrsDTO.setId(id);
        PqrsDTO result = pqrsService.update(pqrsDTO);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST  /public/pqrs} : Create a new public pqrs.
     *
     * @param publicPqrsDTO the publicPqrsDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new publicPqrsDTO,
     *         or with status {@code 400 (Bad Request)} if the publicPqrsDTO is not
     *         valid.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/public/pqrs")
    public ResponseEntity<PublicPqrsDTO> createPublicPqrs(@Valid @RequestBody PublicPqrsDTO publicPqrsDTO) throws URISyntaxException {
        LOG.debug("REST request to create public Pqrs : {}", publicPqrsDTO);
        if (publicPqrsDTO.getAccessToken() != null) {
            throw new BadRequestAlertException("A new public pqrs cannot already have an ID", "pqrs", "idexists");
        }
        PublicPqrsDTO result = pqrsService.createPublicPqrs(publicPqrsDTO);

        URI locationUri = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/public/pqrs/{accessToken}")
            .buildAndExpand(result.getAccessToken())
            .toUri();

        return ResponseEntity.created(locationUri)
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "pqrs", result.getAccessToken()))
            .body(result);
    }

    /**
     * {@code GET /public/pqrs/{access_token}} : get the public pqrs by access
     * token.
     *
     * @param access_token the access token of the publicPqrsDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     *         the publicPqrsDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/public/pqrs/{access_token}")
    public ResponseEntity<PublicPqrsDTO> getPublicPqrsByAccessToken(@PathVariable("access_token") String access_token) {
        LOG.debug("REST request to get public Pqrs by access_token : {}", access_token);
        Optional<PublicPqrsDTO> publicPqrsDTO = pqrsService.findPublicPqrsByAccessToken(access_token);
        return ResponseUtil.wrapOrNotFound(publicPqrsDTO);
    }

    /**
     * {@code POST /public/pqrs/{access_token}/responses} : Submit a new response to
     * a public pqrs.
     *
     * @param access_token      the access token of the Pqrs to respond to.
     * @param publicResponseDTO the publicResponseDTO to create.
     * @param files             optional list of files for the response.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     *         body the new publicResponseDTO.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping(value = "/public/pqrs/{access_token}/responses", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<PublicResponseDTO> submitPublicResponse(
        @PathVariable("access_token") String access_token,
        @RequestPart(value = "response") @Parameter(
            schema = @Schema(type = "string", format = "binary")
        ) PublicResponseDTO publicResponseDTO,
        @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws URISyntaxException {
        LOG.debug("REST request to submit public response for Pqrs with access_token : {}", access_token);

        if (publicResponseDTO.getId() != null) {
            throw new BadRequestAlertException("A new public response cannot already have an ID", "publicResponse", "idexists");
        }
        PublicResponseDTO result = pqrsService.addPublicResponseToPqrs(access_token, publicResponseDTO, files);

        return ResponseEntity.status(HttpStatus.CREATED)
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "pqrs", result.getId()))
            .body(result);
    }
}
