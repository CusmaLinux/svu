package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.SatisfactionSurvey;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.repository.ArchivoAdjuntoRepository;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.repository.SatisfactionSurveyRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.CreateSatisfactionSurveyDTO;
import co.edu.itp.svu.service.dto.OficinaDTO;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicPqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicResponseDTO;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.OficinaMapper;
import co.edu.itp.svu.service.mapper.PqrsMapper;
import co.edu.itp.svu.service.mapper.SatisfactionSurveyMapper;
import co.edu.itp.svu.service.mapper.api.PublicPqrsMapper;
import co.edu.itp.svu.service.mapper.api.PublicResponseMapper;
import co.edu.itp.svu.service.notification.PqrsNotificationService;
import co.edu.itp.svu.service.notification.PqrsNotificationService.PqrsNotificationType;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import co.edu.itp.svu.web.rest.errors.ConflictException;
import co.edu.itp.svu.web.rest.errors.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashSet;
import java.util.List;
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

    private final ArchivoAdjuntoService attachedFileService;

    private final RespuestaRepository responseRepository;

    private final UserRepository userRepository;

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
        ArchivoAdjuntoService attachedFileService,
        OficinaMapper oficinaMapper,
        PqrsNotificationService pqrsNotificationService,
        PublicPqrsMapper publicPqrsMapper,
        RespuestaRepository responseRepository,
        PublicResponseMapper publicResponseMapper,
        SequenceGeneratorService sequenceGenerator,
        MailService mailService,
        UserRepository userRepository,
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
        this.attachedFileService = attachedFileService;
        this.oficinaMapper = oficinaMapper;
        this.pqrsNotificationService = pqrsNotificationService;
        this.publicPqrsMapper = publicPqrsMapper;
        this.responseRepository = responseRepository;
        this.publicResponseMapper = publicResponseMapper;
        this.sequenceGenerator = sequenceGenerator;
        this.mailService = mailService;
        this.userRepository = userRepository;
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
        pqrsRepository.save(pqrs);
    }

    public String suggestOffice(String pqrsId) {
        Pqrs pqrs = pqrsRepository.findById(pqrsId).orElseThrow(() -> new BadRequestAlertException("Pqrs not found", "Pqrs", "idnotfound"));

        List<Oficina> offices = oficinaRepository.findAll();

        Set<ArchivoAdjunto> attachments = attachedFileRepository.findByPqrsAttachment_Id(pqrsId);

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
                pqrsMapper.partialUpdate(existingPqrs, pqrsDTO);
                return existingPqrs;
            })
            .map(pqrsRepository::save)
            .map(pqrsMapper::toDto);
    }

    /**
     * Get all the pqrs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<PqrsDTO> findAll(String state, String idOffice, LocalDate date, Pageable pageable) {
        LOG.debug("Request to get all Pqrs");
        String closedState = "CERRADA";
        boolean isIdOfficeProvided = idOffice != null && !idOffice.trim().isEmpty();

        if (closedState.equals(state) && !isIdOfficeProvided) {
            return pqrsRepository.findAllByEstadoNotAndFechaCreacionLessThanEqual(state, date, pageable).map(pqrsMapper::toDto);
        } else if (!closedState.equals(state) && !isIdOfficeProvided) {
            return pqrsRepository.findAllByEstadoAndFechaCreacionLessThanEqual(state, date, pageable).map(pqrsMapper::toDto);
        } else if (closedState.equals(state) && isIdOfficeProvided) {
            return pqrsRepository
                .findByEstadoNotAndOficinaResponder_IdAndFechaCreacionLessThanEqual(state, idOffice, date, pageable)
                .map(pqrsMapper::toDto);
        } else if (!closedState.equals(state) && isIdOfficeProvided) {
            return pqrsRepository
                .findByEstadoAndOficinaResponder_IdAndFechaCreacionLessThanEqual(state, idOffice, date, pageable)
                .map(pqrsMapper::toDto);
        }
        return pqrsRepository.findAll(pageable).map(pqrsMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<PqrsDTO> search(String criteria, Pageable pageable) {
        LOG.debug("Request to search for a page of Pqrs for query {}", criteria);

        String officeId = null;

        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.FUNCTIONARY)) {
            String currentUserLogin = SecurityUtils.getCurrentUserLogin()
                .orElseThrow(() -> new IllegalStateException("Current user login not found"));

            User currentUser = userRepository
                .findOneByLogin(currentUserLogin)
                .orElseThrow(() -> new IllegalStateException("User not found: " + currentUserLogin));

            String userResponsibleId = currentUser.getId();

            officeId = oficinaRepository.findByResponsable_Id(userResponsibleId).map(Oficina::getId).orElse(null);
        }

        return pqrsRepository.search(criteria, officeId, pageable).map(pqrsMapper::toDto);
    }

    /**
     * Get one pqrs by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<PqrsDTO> findOne(String id) {
        LOG.debug("Request to get Pqrs : {}", id);
        return pqrsRepository.findById(id).map(pqrsMapper::toDto);
    }

    /**
     * Delete the pqrs by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
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
        Page<Pqrs> pqrsPage = pqrsRepository.findAll(pageable);
        return pqrsPage.map(pqrs -> {
            PqrsDTO dto = pqrsMapper.toDto(pqrs);
            Optional<Oficina> oficinaOpt = this.oficinaRepository.findById(dto.getOficinaResponder().getId());
            oficinaOpt.ifPresent(oficina -> {
                OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);
                dto.setOficinaResponder(oficinaDTO);
            });
            return dto;
        });
    }

    public Optional<PqrsDTO> findPqrsById(String id) {
        LOG.debug("Request to get Pqrs : {}", id);
        return pqrsRepository
            .findById(id)
            .map(pqrs -> {
                Set<ArchivoAdjunto> pqrsAttachments = attachedFileRepository.findByPqrsAttachment_Id(pqrs.getId());
                pqrs.set_transientAttachments(pqrsAttachments != null ? pqrsAttachments : new HashSet<>());

                List<Respuesta> responsesFromDb = responseRepository.findByPqrsId(pqrs.getId());
                Set<Respuesta> populatedResponses = new HashSet<>();
                for (Respuesta response : responsesFromDb) {
                    Set<ArchivoAdjunto> responseAttachments = attachedFileRepository.findByResponseAttachment_Id(response.getId());
                    response.set_transientAttachments(responseAttachments != null ? responseAttachments : new HashSet<>());
                    populatedResponses.add(response);
                }
                pqrs.set_transientResponses(populatedResponses);

                PqrsDTO dto = pqrsMapper.toDto(pqrs);
                Optional<Oficina> oficinaOpt = this.oficinaRepository.findById(dto.getOficinaResponder().getId());
                oficinaOpt.ifPresent(oficina -> {
                    OficinaDTO oficinaDTO = oficinaMapper.toDto(oficina);
                    dto.setOficinaResponder(oficinaDTO);
                });
                return dto;
            });
    }

    private PqrsDTO mapPqrsToDtoWithOffice(Pqrs pqrs) {
        PqrsDTO dto = pqrsMapper.toDto(pqrs);
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

    public PqrsDTO create(PqrsDTO pqrsDTO) {
        LOG.debug("Request to create a Pqrs: {}", pqrsDTO);
        Pqrs pqrs = pqrsMapper.toEntity(pqrsDTO);
        pqrs.setEstado(PqrsStatus.PENDING.getDisplayName());

        Instant globalCurrentDate = Instant.now();
        pqrs.setFechaCreacion(globalCurrentDate);

        Instant dueDate = globalCurrentDate.plus(15, ChronoUnit.DAYS);
        pqrs.setFechaLimiteRespuesta(dueDate);

        Oficina office = oficinaRepository.findByNombre("Ventanilla Unica");
        pqrs.setOficinaResponder(office);

        if (pqrsDTO.getArchivosAdjuntosDTO() != null) {
            Set<ArchivoAdjunto> archivosAdjuntos = pqrsDTO
                .getArchivosAdjuntosDTO()
                .stream()
                .map(this::convertToEntity)
                .collect(Collectors.toSet());
            pqrs.setArchivosAdjuntos(archivosAdjuntos);
        }

        pqrs = pqrsRepository.save(pqrs);

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.FRONT_DESK_CS, AuthoritiesConstants.ADMIN)) {
            pqrsNotificationService.sendPqrsNotification(
                pqrs,
                PqrsNotificationService.PqrsNotificationType.PQRS_CREATED,
                AuthoritiesConstants.FRONT_DESK_CS
            );
        }

        return mapPqrsToDtoWithOffice(pqrs);
    }

    private ArchivoAdjunto convertToEntity(ArchivoAdjuntoDTO dto) {
        ArchivoAdjunto archivo = new ArchivoAdjunto();
        archivo.setId(dto.getId());
        archivo.setNombre(dto.getNombre());
        archivo.setTipo(dto.getTipo());
        archivo.setUrlArchivo(dto.getUrlArchivo());
        archivo.setFechaSubida(dto.getFechaSubida());

        return archivo;
    }

    public PqrsDTO update(PqrsDTO pqrsDTO) {
        Pqrs pqrs = pqrsMapper.toEntity(pqrsDTO);
        LOG.info("pqrs --------------------------------- {}", pqrs.toString());
        Pqrs oldPqrs = pqrsRepository.findById(pqrs.getId()).orElse(null);

        if (SecurityUtils.isAuthenticated()) {
            if (PqrsStatus.RESOLVED.getDisplayName().equalsIgnoreCase(pqrs.getEstado())) {
                boolean changeState = !Objects.equals(oldPqrs.getEstado(), pqrs.getEstado());

                if (changeState) {
                    pqrsNotificationService.sendPqrsNotification(
                        pqrs,
                        PqrsNotificationType.PQRS_RESOLVED,
                        AuthoritiesConstants.FRONT_DESK_CS
                    );
                }
            }

            boolean changeOffice = !Objects.equals(oldPqrs.getOficinaResponder().getId(), pqrs.getOficinaResponder().getId());

            if (changeOffice) {
                Oficina office = oficinaRepository.findById(pqrs.getOficinaResponder().getId()).orElse(null);
                User responsible = office.getResponsable();
                pqrsNotificationService.sendPqrsNotification(pqrs, PqrsNotificationType.PQRS_ASSIGNED, responsible);
            }

            boolean changeDeadline = !Objects.equals(oldPqrs.getDaysToReply(), pqrs.getDaysToReply());
            if (changeDeadline) {
                Instant newDeadline = deadlineCalculationService.calculateDeadline(oldPqrs.getFechaCreacion(), pqrs.getDaysToReply());
                pqrs.setFechaLimiteRespuesta(newDeadline);
            }
        }

        Pqrs savedPqrs = pqrsRepository.save(pqrs);
        Set<ArchivoAdjunto> successfullyLinkedAttachments = new HashSet<>();

        if (pqrsDTO.get_transientAttachments() != null && !pqrsDTO.get_transientAttachments().isEmpty()) {
            for (ArchivoAdjuntoDTO adjuntoDTO : pqrsDTO.get_transientAttachments()) {
                if (adjuntoDTO.getId() != null) {
                    Optional<ArchivoAdjunto> adjuntoOpt = attachedFileRepository.findById(adjuntoDTO.getId());
                    adjuntoOpt.ifPresentOrElse(
                        adjuntoToLink -> {
                            adjuntoToLink.setPqrsAttachment(savedPqrs);
                            attachedFileRepository.save(adjuntoToLink);
                            successfullyLinkedAttachments.add(adjuntoToLink);
                        },
                        () ->
                            LOG.warn(
                                "ArchivoAdjunto with ID {} provided in DTO not found. Cannot link to PQRS {}.",
                                adjuntoDTO.getId(),
                                savedPqrs.getId()
                            )
                    );
                } else {
                    LOG.warn("ArchivoAdjuntoDTO in _transientAttachments is missing an ID. Cannot link.");
                }
            }
        }

        if (!successfullyLinkedAttachments.isEmpty()) {
            savedPqrs.set_transientAttachments(successfullyLinkedAttachments);
        } else {
            savedPqrs.set_transientAttachments(new HashSet<>());
        }

        return pqrsMapper.toDto(savedPqrs);
    }

    public PublicPqrsDTO createPublicPqrs(PublicPqrsDTO publicPqrsDTO) {
        LOG.debug("Request to create public Pqrs : {}", publicPqrsDTO);
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

        Set<ArchivoAdjunto> successfullyLinkedAttachments = new HashSet<>();

        if (publicPqrsDTO.get_transientAttachments() != null && !publicPqrsDTO.get_transientAttachments().isEmpty()) {
            for (ArchivoAdjuntoDTO adjuntoDTO : publicPqrsDTO.get_transientAttachments()) {
                if (adjuntoDTO.getId() != null) {
                    Optional<ArchivoAdjunto> adjuntoOpt = attachedFileRepository.findById(adjuntoDTO.getId());
                    adjuntoOpt.ifPresentOrElse(
                        adjuntoToLink -> {
                            adjuntoToLink.setPqrsAttachment(savedPqrs);
                            attachedFileRepository.save(adjuntoToLink);
                            successfullyLinkedAttachments.add(adjuntoToLink);
                        },
                        () ->
                            LOG.warn(
                                "ArchivoAdjunto with ID {} provided in DTO not found. Cannot link to PQRS {}.",
                                adjuntoDTO.getId(),
                                savedPqrs.getId()
                            )
                    );
                } else {
                    LOG.warn("ArchivoAdjuntoDTO in _transientAttachments is missing an ID. Cannot link.");
                }
            }
        }

        if (!successfullyLinkedAttachments.isEmpty()) {
            savedPqrs.set_transientAttachments(successfullyLinkedAttachments);
        } else {
            savedPqrs.set_transientAttachments(new HashSet<>());
        }

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

        return publicPqrsMapper.toDto(savedPqrs);
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
        LOG.debug("Request to get public Pqrs by access token : {}", accessToken);
        Optional<Pqrs> pqrsOpt = pqrsRepository.findByAccessToken(accessToken);

        return pqrsOpt.map(pqrs -> {
            Set<ArchivoAdjunto> pqrsAttachments = attachedFileRepository.findByPqrsAttachment_Id(pqrs.getId());
            pqrs.set_transientAttachments(pqrsAttachments != null ? pqrsAttachments : new HashSet<>());

            List<Respuesta> responsesFromDb = responseRepository.findByPqrsId(pqrs.getId());
            Set<Respuesta> populatedResponses = new HashSet<>();
            for (Respuesta response : responsesFromDb) {
                Set<ArchivoAdjunto> responseAttachments = attachedFileRepository.findByResponseAttachment_Id(response.getId());
                response.set_transientAttachments(responseAttachments != null ? responseAttachments : new HashSet<>());
                populatedResponses.add(response);
            }
            pqrs.set_transientResponses(populatedResponses);

            return publicPqrsMapper.toDto(pqrs);
        });
    }

    public PublicResponseDTO addPublicResponseToPqrs(String accessToken, PublicResponseDTO publicResponseDTO, List<MultipartFile> files) {
        LOG.debug("Request to add public response to Pqrs with access token: {}", accessToken);
        Pqrs pqrs = pqrsRepository
            .findByAccessToken(accessToken)
            .orElseThrow(() -> new BadRequestAlertException("Pqrs not found for the given access token", "Pqrs", "accessTokenNotFound"));

        Respuesta response = new Respuesta();
        response.setContenido(publicResponseDTO.getContenido());
        response.setFechaRespuesta(Instant.now());
        response.setPqrs(pqrs);
        response.setResolver(null);

        response = responseRepository.save(response);

        Set<ArchivoAdjunto> savedAttachments = new HashSet<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ArchivoAdjunto attachedFile = attachedFileService.saveFileMetadata(file);
                    attachedFile.setResponseAttachment(response);

                    savedAttachments.add(attachedFileRepository.save(attachedFile));
                }
            }
        }

        response.set_transientAttachments(savedAttachments);
        return publicResponseMapper.toDto(response);
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
                Pqrs updatedPqrs = pqrsRepository.save(pqrs);

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
