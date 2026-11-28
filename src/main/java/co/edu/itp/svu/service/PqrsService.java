package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.SatisfactionSurvey;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.repository.ArchivoAdjuntoRepository;
import co.edu.itp.svu.repository.AttachmentPublicationVisibilityRepository;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.repository.SatisfactionSurveyRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.CreateSatisfactionSurveyDTO;
import co.edu.itp.svu.service.dto.OficinaDTO;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicPqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicResponseDTO;
import co.edu.itp.svu.service.errors.BadRequestAlertException;
import co.edu.itp.svu.service.errors.ConflictException;
import co.edu.itp.svu.service.errors.ResourceNotFoundException;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.OficinaMapper;
import co.edu.itp.svu.service.mapper.PqrsMapper;
import co.edu.itp.svu.service.mapper.SatisfactionSurveyMapper;
import co.edu.itp.svu.service.mapper.api.PublicPqrsMapper;
import co.edu.itp.svu.service.mapper.api.PublicResponseMapper;
import co.edu.itp.svu.service.notification.PqrsNotificationService;
import co.edu.itp.svu.service.notification.PqrsNotificationService.PqrsNotificationType;
import co.edu.itp.svu.service.storage.AttachmentOperationCoordinator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service Implementation for managing {@link co.edu.itp.svu.domain.Pqrs}.
 */
@Service
public class PqrsService {

    private static final Logger LOG = LoggerFactory.getLogger(PqrsService.class);

    private final PqrsMapper pqrsMapper;

    private final PqrsRepository pqrsRepository;

    private final OficinaRepository oficinaRepository;

    private final ArchivoAdjuntoRepository attachedFileRepository;

    private final AttachmentPublicationVisibilityRepository attachmentVisibility;

    private final AttachmentOperationCoordinator attachmentOperations;

    private final ArchivoAdjuntoService attachedFileService;

    private final RespuestaRepository responseRepository;

    private final PqrsAuthorizationService authorization;

    private final PqrsNotificationService pqrsNotificationService;

    private final PublicPqrsMapper publicPqrsMapper;

    private final PublicResponseMapper publicResponseMapper;

    private OficinaMapper oficinaMapper;

    private final SequenceGeneratorService sequenceGenerator;

    private final MailService mailService;

    private final DeadlineCalculationService deadlineCalculationService;

    private final GeminiService geminiService;

    private final FileExtractorService fileExtractorService;

    private final SatisfactionSurveyRepository satisfactionSurveyRepository;

    private final SatisfactionSurveyMapper satisfactionSurveyMapper;

    public PqrsService(
        PqrsRepository pqrsRepository,
        PqrsMapper pqrsMapper,
        ArchivoAdjuntoMapper archivoAdjuntoMapper,
        OficinaRepository oficinaRepository,
        ArchivoAdjuntoRepository attachedFileRepository,
        AttachmentPublicationVisibilityRepository attachmentVisibility,
        AttachmentOperationCoordinator attachmentOperations,
        ArchivoAdjuntoService attachedFileService,
        OficinaMapper oficinaMapper,
        PqrsNotificationService pqrsNotificationService,
        PublicPqrsMapper publicPqrsMapper,
        RespuestaRepository responseRepository,
        PublicResponseMapper publicResponseMapper,
        SequenceGeneratorService sequenceGenerator,
        MailService mailService,
        PqrsAuthorizationService authorization,
        DeadlineCalculationService deadlineCalculationService,
        GeminiService geminiService,
        FileExtractorService fileExtractorService,
        SatisfactionSurveyRepository satisfactionSurveyRepository,
        SatisfactionSurveyMapper satisfactionSurveyMapper
    ) {
        this.pqrsRepository = pqrsRepository;
        this.pqrsMapper = pqrsMapper;
        this.oficinaRepository = oficinaRepository;
        this.attachedFileRepository = attachedFileRepository;
        this.attachmentVisibility = attachmentVisibility;
        this.attachmentOperations = attachmentOperations;
        this.attachedFileService = attachedFileService;
        this.oficinaMapper = oficinaMapper;
        this.pqrsNotificationService = pqrsNotificationService;
        this.publicPqrsMapper = publicPqrsMapper;
        this.responseRepository = responseRepository;
        this.publicResponseMapper = publicResponseMapper;
        this.sequenceGenerator = sequenceGenerator;
        this.mailService = mailService;
        this.authorization = authorization;
        this.deadlineCalculationService = deadlineCalculationService;
        this.geminiService = geminiService;
        this.fileExtractorService = fileExtractorService;
        this.satisfactionSurveyRepository = satisfactionSurveyRepository;
        this.satisfactionSurveyMapper = satisfactionSurveyMapper;
    }

    public void saveSatisfactionSurvey(String accessToken, CreateSatisfactionSurveyDTO createSurveyDTO) {
        Pqrs pqrs = pqrsRepository
            .findByAccessToken(accessToken)
            .orElseThrow(() -> new ResourceNotFoundException("error.pqrs.notFoundWithToken"));

        if (!PqrsStatus.RESOLVED.getDisplayName().equals(pqrs.getEstado())) {
            throw new ConflictException("error.pqrs.surveyNotResolved");
        }
        if (pqrs.getSatisfactionSurvey() != null) {
            throw new ConflictException("error.pqrs.surveyAlreadySubmitted");
        }

        SatisfactionSurvey survey = new SatisfactionSurvey();
        survey.setRating(createSurveyDTO.getRating());
        survey.setComment(createSurveyDTO.getComment());

        survey.setSubmissionDate(Instant.now());
        survey.setPqrs(pqrs);

        satisfactionSurveyRepository.save(survey);

        pqrs.setSatisfactionSurvey(survey);
        if (!pqrsRepository.updateSatisfactionSurvey(pqrs.getId(), survey)) throw new ConflictException(
            "The case changed; retry the survey"
        );
    }

    public String suggestOffice(String pqrsId) {
        Pqrs pqrs = pqrsRepository.findById(pqrsId).orElseThrow(() -> new BadRequestAlertException("Pqrs not found", "Pqrs", "idnotfound"));
        authorization.checkCaseReadAccess(pqrs);

        List<Oficina> offices = oficinaRepository.findAll();

        Set<ArchivoAdjunto> attachments = visibleAttachments(attachedFileRepository.findByPqrsAttachment_Id(pqrsId));

        String fileText = fileExtractorService.extractTextFromAttachments(attachments);

        return geminiService.suggestOffice(pqrs, offices, fileText);
    }

    /**
     * Partially update a pqrs.
     *
     * @param pqrsDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PqrsDTO> partialUpdate(PqrsDTO pqrsDTO) {
        LOG.debug("Request to partially update Pqrs : {}", pqrsDTO);
        return pqrsRepository
            .findById(pqrsDTO.getId())
            .map(existingPqrs -> {
                // Partial updates can modify sensitive fields; only privileged staff may use this endpoint.
                authorization.requireAdministrativeCaseAccess();
                long expectedRevision = existingPqrs.getAuthorizationRevision();
                Pqrs.Publication publication = existingPqrs.getPublication();
                Pqrs.ResponsePublicationLock lock = existingPqrs.getResponsePublicationLock();
                Set<ArchivoAdjunto> currentAttachments = existingPqrs.getArchivosAdjuntos();
                validateExistingAttachmentIds(pqrsDTO.getArchivosAdjuntosDTO(), existingPqrs.getId());
                pqrsMapper.partialUpdate(existingPqrs, pqrsDTO);
                existingPqrs.setArchivosAdjuntos(currentAttachments);
                existingPqrs.setPublication(publication);
                existingPqrs.setResponsePublicationLock(lock);
                existingPqrs.setAuthorizationRevision(expectedRevision);
                boolean officeChanged =
                    pqrsDTO.getOficinaResponder() != null &&
                    !Objects.equals(
                        existingPqrs.getOficinaResponder() == null ? null : existingPqrs.getOficinaResponder().getId(),
                        pqrsDTO.getOficinaResponder().getId()
                    );
                if (officeChanged) existingPqrs.setAuthorizationRevision(expectedRevision + 1);
                if (!pqrsRepository.replaceIfAuthorizationCurrent(existingPqrs, expectedRevision, officeChanged)) {
                    throw new ConflictException("The case assignment or publication changed; reload the case");
                }
                return existingPqrs;
            })
            .map(this::toPqrsDto);
    }

    public Page<PqrsDTO> findAll(String state, String idOffice, LocalDate date, Pageable pageable) {
        Optional<String> office = authorization.requireOfficeFilter(idOffice);
        boolean excludeState = "CERRADA".equals(state);
        Page<Pqrs> page;
        if (office.isPresent()) {
            String officeId = office.orElseThrow();
            page = excludeState
                ? pqrsRepository.findByEstadoNotAndOficinaResponder_IdAndFechaCreacionLessThanEqual(state, officeId, date, pageable)
                : pqrsRepository.findByEstadoAndOficinaResponder_IdAndFechaCreacionLessThanEqual(state, officeId, date, pageable);
        } else {
            page = excludeState
                ? pqrsRepository.findAllByEstadoNotAndFechaCreacionLessThanEqual(state, date, pageable)
                : pqrsRepository.findAllByEstadoAndFechaCreacionLessThanEqual(state, date, pageable);
        }
        return page.map(this::toPqrsDto);
    }

    @Transactional(readOnly = true)
    public Page<PqrsDTO> search(String criteria, Pageable pageable) {
        LOG.debug("Request to search for a page of Pqrs for query {}", criteria);

        Optional<String> officeId = authorization.requireOperationalOffice();

        return pqrsRepository.search(criteria, officeId, pageable).map(this::toPqrsDto);
    }

    /**
     * Get one pqrs by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<PqrsDTO> findOne(String id) {
        LOG.debug("Request to get Pqrs : {}", id);
        return pqrsRepository
            .findById(id)
            .map(pqrs -> {
                authorization.checkCaseReadAccess(pqrs);
                return toPqrsDto(pqrs);
            });
    }

    /**
     * Delete the pqrs by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        authorization.requireAdministrativeCaseAccess();
        LOG.debug("Request to delete Pqrs : {}", id);
        pqrsRepository.deleteById(id);
    }

    /**
     * Get all the pqrs with oficina.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<PqrsDTO> findAllOficina(Pageable pageable) {
        LOG.debug("Request to get all Pqrs");
        return pqrsRepository.search("", authorization.requireOperationalOffice(), pageable).map(this::mapPqrsToDtoWithOffice);
    }

    public Optional<PqrsDTO> findPqrsById(String id) {
        LOG.debug("Request to get Pqrs : {}", id);
        return pqrsRepository
            .findById(id)
            .map(pqrs -> {
                authorization.checkCaseReadAccess(pqrs);
                Set<ArchivoAdjunto> pqrsAttachments = visibleAttachments(attachedFileRepository.findByPqrsAttachment_Id(pqrs.getId()));
                pqrs.set_transientAttachments(pqrsAttachments != null ? pqrsAttachments : new HashSet<>());

                List<Respuesta> responsesFromDb = visibleResponses(pqrs.getId());
                Set<Respuesta> populatedResponses = new HashSet<>();
                for (Respuesta response : responsesFromDb) {
                    Set<ArchivoAdjunto> responseAttachments = visibleAttachments(
                        attachedFileRepository.findByResponseAttachment_Id(response.getId())
                    );
                    response.set_transientAttachments(responseAttachments != null ? responseAttachments : new HashSet<>());
                    populatedResponses.add(response);
                }
                pqrs.set_transientResponses(populatedResponses);

                PqrsDTO dto = toPqrsDto(pqrs);
                if (dto.getOficinaResponder() != null && dto.getOficinaResponder().getId() != null) {
                    Optional<Oficina> oficinaOpt = this.oficinaRepository.findById(dto.getOficinaResponder().getId());
                    oficinaOpt.ifPresent(oficina -> {
                        OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);
                        dto.setOficinaResponder(oficinaDTO);
                    });
                }
                return dto;
            });
    }

    private PqrsDTO mapPqrsToDtoWithOffice(Pqrs pqrs) {
        PqrsDTO dto = toPqrsDto(pqrs);
        if (pqrs.getOficinaResponder() != null && pqrs.getOficinaResponder().getId() != null) {
            oficinaRepository
                .findById(pqrs.getOficinaResponder().getId())
                .ifPresent(oficina -> {
                    OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);
                    dto.setOficinaResponder(oficinaDTO);
                });
        }
        return dto;
    }

    private PqrsDTO toPqrsDto(Pqrs pqrs) {
        PqrsDTO dto = pqrsMapper.toDto(pqrs);
        hideManagedKeys(dto.getArchivosAdjuntosDTO());
        hideManagedKeys(dto.get_transientAttachments());
        if (dto.get_transientResponses() != null) dto
            .get_transientResponses()
            .forEach(response -> hideManagedKeys(response.get_transientAttachments()));
        return dto;
    }

    private PublicPqrsDTO hideManagedKeys(PublicPqrsDTO dto) {
        if (dto == null) return null;
        hideManagedKeys(dto.get_transientAttachments());
        if (dto.get_transientResponses() != null) dto.get_transientResponses().forEach(this::hideManagedKeys);
        return dto;
    }

    private PublicResponseDTO hideManagedKeys(PublicResponseDTO dto) {
        if (dto != null) hideManagedKeys(dto.get_transientAttachments());
        return dto;
    }

    private void hideManagedKeys(Set<ArchivoAdjuntoDTO> attachments) {
        if (attachments == null) return;
        for (ArchivoAdjuntoDTO attachment : attachments) {
            if (attachment != null && attachment.getId() != null && attachmentVisibility.isManaged(attachment.getId())) {
                attachment.setUrlArchivo(null);
            }
        }
    }

    public PqrsDTO create(PqrsDTO pqrsDTO) {
        authorization.requireAdministrativeCaseAccess();
        if (pqrsDTO.getArchivosAdjuntosDTO() != null && !pqrsDTO.getArchivosAdjuntosDTO().isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException("Attachments must be sent with the secure operation");
        }
        LOG.debug("Request to create a Pqrs: {}", pqrsDTO);
        Pqrs pqrs = pqrsMapper.toEntity(pqrsDTO);
        pqrs.setEstado(PqrsStatus.PENDING.getDisplayName());

        Instant globalCurrentDate = Instant.now();
        pqrs.setFechaCreacion(globalCurrentDate);

        Instant dueDate = globalCurrentDate.plus(15, ChronoUnit.DAYS);
        pqrs.setFechaLimiteRespuesta(dueDate);

        Oficina office = oficinaRepository.findByNombre("Ventanilla Unica");
        pqrs.setOficinaResponder(office);

        pqrs = pqrsRepository.save(pqrs);

        pqrsNotificationService.sendPqrsNotification(
            pqrs,
            PqrsNotificationService.PqrsNotificationType.PQRS_CREATED,
            AuthoritiesConstants.FRONT_DESK_CS
        );

        return mapPqrsToDtoWithOffice(pqrs);
    }

    public PqrsDTO update(PqrsDTO pqrsDTO) {
        Pqrs pqrs = pqrsMapper.toEntity(pqrsDTO);
        LOG.info("pqrs --------------------------------- {}", pqrs.toString());
        Pqrs oldPqrs = pqrsRepository.findById(pqrs.getId()).orElse(null);

        if (oldPqrs == null) {
            throw new BadRequestAlertException("Pqrs not found", "Pqrs", "idnotfound");
        }
        // The marker is intentionally absent from REST DTOs; legacy edits must preserve it.
        validateExistingAttachmentIds(pqrsDTO.getArchivosAdjuntosDTO(), oldPqrs.getId());
        pqrs.setArchivosAdjuntos(oldPqrs.getArchivosAdjuntos());
        pqrs.setPublication(oldPqrs.getPublication());
        pqrs.setResponsePublicationLock(oldPqrs.getResponsePublicationLock());
        pqrs.setAuthorizationRevision(oldPqrs.getAuthorizationRevision());
        authorization.checkCaseWriteAccess(oldPqrs, pqrs);
        boolean changeOffice = !Objects.equals(
            oldPqrs.getOficinaResponder() == null ? null : oldPqrs.getOficinaResponder().getId(),
            pqrs.getOficinaResponder() == null ? null : pqrs.getOficinaResponder().getId()
        );
        if (changeOffice) pqrs.setAuthorizationRevision(oldPqrs.getAuthorizationRevision() + 1);
        boolean changeDeadline = !Objects.equals(oldPqrs.getDaysToReply(), pqrs.getDaysToReply());
        if (changeDeadline) pqrs.setFechaLimiteRespuesta(
            deadlineCalculationService.calculateDeadline(oldPqrs.getFechaCreacion(), pqrs.getDaysToReply())
        );

        validateExistingAttachmentIds(pqrsDTO.get_transientAttachments(), pqrs.getId());

        if (!pqrsRepository.replaceIfAuthorizationCurrent(pqrs, oldPqrs.getAuthorizationRevision(), changeOffice)) {
            throw new ConflictException("The case assignment or publication changed; reload the case");
        }
        if (SecurityUtils.isAuthenticated()) {
            if (
                PqrsStatus.RESOLVED.getDisplayName().equalsIgnoreCase(pqrs.getEstado()) &&
                !Objects.equals(oldPqrs.getEstado(), pqrs.getEstado())
            ) {
                pqrsNotificationService.sendPqrsNotification(
                    pqrs,
                    PqrsNotificationService.PqrsNotificationType.PQRS_RESOLVED,
                    AuthoritiesConstants.FRONT_DESK_CS
                );
            }
            if (changeOffice) {
                oficinaRepository
                    .findById(pqrs.getOficinaResponder().getId())
                    .map(Oficina::getResponsable)
                    .ifPresent(responsible ->
                        pqrsNotificationService.sendPqrsNotification(pqrs, PqrsNotificationType.PQRS_ASSIGNED, responsible)
                    );
            }
        }
        return toPqrsDto(pqrs);
    }

    private void validateExistingAttachmentIds(Set<ArchivoAdjuntoDTO> submitted, String caseId) {
        if (submitted == null) return;
        Set<String> existingIds = attachedFileRepository
            .findByPqrsAttachment_Id(caseId)
            .stream()
            .filter(file -> attachmentVisibility.isVisible(file.getId()))
            .map(ArchivoAdjunto::getId)
            .collect(Collectors.toSet());
        if (submitted.stream().anyMatch(file -> file == null || !existingIds.contains(file.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Attachment associations require the operation flow");
        }
    }

    public PublicPqrsDTO createPublicPqrs(PublicPqrsDTO publicPqrsDTO) {
        rejectAttachmentIdentifiers(publicPqrsDTO.get_transientAttachments());
        Pqrs pqrs = publicPqrsMapper.toEntity(publicPqrsDTO);

        Instant globalCurrentDate = Instant.now();

        pqrs.setEstado(PqrsStatus.PENDING.getDisplayName());
        pqrs.setDaysToReply(15);
        pqrs.setAccessToken(UUID.randomUUID().toString());
        pqrs.setFechaCreacion(globalCurrentDate);
        pqrs.setFileNumber(generateFileNumber());

        Instant deadline = deadlineCalculationService.calculateDeadline(pqrs.getFechaCreacion(), pqrs.getDaysToReply());
        pqrs.setFechaLimiteRespuesta(deadline);

        Oficina office = oficinaRepository.findByNombre("Ventanilla Unica");
        pqrs.setOficinaResponder(office);

        Pqrs savedPqrs = pqrsRepository.save(pqrs);

        savedPqrs.set_transientAttachments(new HashSet<>());

        if (pqrs != null) {
            pqrsNotificationService.sendPqrsNotification(
                pqrs,
                PqrsNotificationService.PqrsNotificationType.PQRS_CREATED,
                AuthoritiesConstants.FRONT_DESK_CS
            );
        }

        if (pqrs.getRequesterEmail() != null) {
            mailService.sendAccessToken(savedPqrs);
        }

        return hideManagedKeys(publicPqrsMapper.toDto(savedPqrs));
    }

    public PublicPqrsDTO createPublicPqrs(PublicPqrsDTO input, List<MultipartFile> files, String requestKey) {
        if (input.getAccessToken() != null) throw new BadRequestAlertException(
            "A new public PQRSD cannot include an access token",
            "pqrs",
            "idexists"
        );
        rejectAttachmentIdentifiers(input.get_transientAttachments());
        Pqrs pqrs = publicPqrsMapper.toEntity(input);
        Instant now = Instant.now();
        pqrs.setEstado(PqrsStatus.PENDING.getDisplayName());
        pqrs.setDaysToReply(15);
        pqrs.setAccessToken(UUID.randomUUID().toString());
        pqrs.setFechaCreacion(now);
        pqrs.setFileNumber(generateFileNumber());
        pqrs.setFechaLimiteRespuesta(deadlineCalculationService.calculateDeadline(now, 15));
        Oficina office = oficinaRepository.findByNombre("Ventanilla Unica");
        if (office == null) throw new IllegalStateException("Default intake office is unavailable");
        pqrs.setOficinaResponder(office);
        Map<String, String> identityFields = Map.of(
            "type",
            String.valueOf(input.getType()),
            "title",
            input.getTitulo(),
            "description",
            input.getDescripcion() == null ? "" : input.getDescripcion(),
            "requesterEmail",
            input.getRequesterEmail() == null ? "" : input.getRequesterEmail()
        );
        Pqrs created = attachmentOperations.publishCase(pqrs, files, requestKey, List.of("PUBLIC_CASE_CREATE"), identityFields);
        String operationId = created.getPublication().getOperationId();
        pqrsNotificationService.sendPqrsNotificationOnce(
            created,
            PqrsNotificationType.PQRS_CREATED,
            AuthoritiesConstants.FRONT_DESK_CS,
            operationId
        );
        if (created.getRequesterEmail() != null) mailService.sendAccessToken(created);
        return hideManagedKeys(publicPqrsMapper.toDto(created));
    }

    /**
     * Generates a unique, sequential "numero de radicado" for a new PQRS.
     *
     * The number is formatted as {@code R<yyyyMMdd><#####>}, where:
     * <ul>
     * <li>{@code R} is a static prefix.</li>
     * <li>{@code yyyyMMdd} is the current date.</li>
     * <li>{@code #####} is a 5-digit padded consecutive number that resets
     * quarterly.</li>
     * </ul>
     * This method relies on the {@link SequenceGeneratorService} to atomically
     * generate the consecutive number, ensuring uniqueness even under concurrent
     * requests.
     *
     * @return A unique {@code String} representing the file number.
     */
    private String generateFileNumber() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        int quarter = today.get(IsoFields.QUARTER_OF_YEAR);
        String sequenceKey = String.format("PQRS_%d_Q%d", year, quarter);

        long consecutiveNumber = sequenceGenerator.getNextSequence(sequenceKey);

        String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("R%s%05d", formattedDate, consecutiveNumber);
    }

    @Transactional(readOnly = true)
    public Optional<PublicPqrsDTO> findPublicPqrsByAccessToken(String accessToken) {
        Optional<Pqrs> pqrsOpt = pqrsRepository.findByAccessToken(accessToken);

        return pqrsOpt.map(pqrs -> {
            Set<ArchivoAdjunto> pqrsAttachments = visibleAttachments(attachedFileRepository.findByPqrsAttachment_Id(pqrs.getId()));
            pqrs.set_transientAttachments(pqrsAttachments != null ? pqrsAttachments : new HashSet<>());

            List<Respuesta> responsesFromDb = visibleResponses(pqrs.getId());
            Set<Respuesta> populatedResponses = new HashSet<>();
            for (Respuesta response : responsesFromDb) {
                Set<ArchivoAdjunto> responseAttachments = visibleAttachments(
                    attachedFileRepository.findByResponseAttachment_Id(response.getId())
                );
                response.set_transientAttachments(responseAttachments != null ? responseAttachments : new HashSet<>());
                populatedResponses.add(response);
            }
            pqrs.set_transientResponses(populatedResponses);

            return hideManagedKeys(publicPqrsMapper.toDto(pqrs));
        });
    }

    private List<Respuesta> visibleResponses(String pqrsId) {
        return responseRepository
            .findByPqrsId(pqrsId)
            .stream()
            .filter(response -> response.getPublication() == null || !"PREPARING".equals(response.getPublication().getState()))
            .toList();
    }

    private Set<ArchivoAdjunto> visibleAttachments(Set<ArchivoAdjunto> attachments) {
        if (attachments == null) return new HashSet<>();
        return attachments.stream().filter(file -> attachmentVisibility.isVisible(file.getId())).collect(Collectors.toSet());
    }

    public PublicResponseDTO addPublicResponseToPqrs(
        String accessToken,
        PublicResponseDTO publicResponseDTO,
        List<MultipartFile> files,
        String requestKey
    ) {
        Pqrs pqrs = pqrsRepository
            .findByAccessToken(accessToken)
            .orElseThrow(() -> new BadRequestAlertException("Pqrs not found for the given access token", "Pqrs", "accessTokenNotFound"));
        rejectAttachmentIdentifiers(publicResponseDTO.get_transientAttachments());
        Respuesta response = new Respuesta();
        response.setContenido(publicResponseDTO.getContenido());
        response.setFechaRespuesta(Instant.now());
        response.setPqrs(pqrs);
        response.setResolver(null);
        response = attachmentOperations.publishResponse(
            pqrs,
            response,
            files,
            requestKey,
            List.of("PUBLIC_RESPONSE", accessToken),
            Map.of("content", publicResponseDTO.getContenido() == null ? "" : publicResponseDTO.getContenido()),
            AttachmentOperationType.CITIZEN_RESPONSE_CREATE,
            null
        );
        return hideManagedKeys(publicResponseMapper.toDto(response));
    }

    private static void rejectAttachmentIdentifiers(Set<ArchivoAdjuntoDTO> attachments) {
        if (
            attachments != null &&
            attachments.stream().anyMatch(file -> file == null || file.getId() != null || file.getUrlArchivo() != null)
        ) {
            throw new org.springframework.security.access.AccessDeniedException("Attachment IDs are not accepted for publication");
        }
    }

    /**
     * Finds the accessToken for a given fileNumber.
     *
     * @param fileNumber the file number to search for.
     * @return an Optional containing the accessToken string if the file is found,
     *         otherwise empty.
     */
    @Transactional(readOnly = true)
    public Optional<String> findAccessTokenByFileNumber(String fileNumber) {
        LOG.debug("Request to find accessToken for file number: {}", fileNumber);

        Optional<Pqrs> pqrsOptional = pqrsRepository.findByFileNumber(fileNumber);

        return pqrsOptional.map(Pqrs::getAccessToken);
    }

    /**
     * Updates the office of an existing pqrs.
     *
     * @param pqrsId     the id of the pqrs to update.
     * @param officeName the name of the new office.
     * @return the updated pqrs as a DTO.
     */
    @Transactional
    public Optional<PqrsDTO> updatePqrsOffice(String pqrsId, String officeName) {
        authorization.requireAdministrativeCaseAccess();
        LOG.debug("Request to update office for Pqrs ID: {} to office: {}", pqrsId, officeName);

        return pqrsRepository
            .findById(pqrsId)
            .map(pqrs -> {
                Oficina newOffice = oficinaRepository.findByNombre(officeName);
                if (newOffice == null) {
                    throw new BadRequestAlertException("Office not found with name: " + officeName, "Oficina", "namenotfound");
                }

                Oficina oldOffice = pqrs.getOficinaResponder();

                if (oldOffice != null && Objects.equals(oldOffice.getId(), newOffice.getId())) {
                    LOG.debug("Pqrs {} is already assigned to office {}. No update needed.", pqrsId, officeName);
                    return pqrs;
                }

                pqrs.setOficinaResponder(newOffice);
                long expectedRevision = pqrs.getAuthorizationRevision();
                pqrs.setAuthorizationRevision(expectedRevision + 1);
                if (!pqrsRepository.replaceIfAuthorizationCurrent(pqrs, expectedRevision, true)) {
                    throw new ConflictException("A response publication is in progress; retry reassignment");
                }
                Pqrs updatedPqrs = pqrs;

                if (oldOffice == null || !Objects.equals(oldOffice.getId(), newOffice.getId())) {
                    User responsible = newOffice.getResponsable();
                    if (responsible != null) {
                        pqrsNotificationService.sendPqrsNotification(updatedPqrs, PqrsNotificationType.PQRS_ASSIGNED, responsible);
                    } else {
                        LOG.warn("No responsible user found for office '{}' to send notification.", officeName);
                    }
                }
                return updatedPqrs;
            })
            .map(pqrsMapper::toDto);
    }
}
