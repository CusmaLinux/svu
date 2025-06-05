package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.repository.ArchivoAdjuntoRepository;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.OficinaDTO;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicPqrsDTO;
import co.edu.itp.svu.service.dto.api.PublicResponseDTO;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.OficinaMapper;
import co.edu.itp.svu.service.mapper.PqrsMapper;
import co.edu.itp.svu.service.mapper.api.PublicPqrsMapper;
import co.edu.itp.svu.service.mapper.api.PublicResponseMapper;
import co.edu.itp.svu.service.notification.PqrsNotificationService;
import co.edu.itp.svu.service.notification.PqrsNotificationService.PqrsNotificationType;
import co.edu.itp.svu.web.rest.errors.BadRequestAlertException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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

    private final PqrsNotificationService pqrsNotificationService;

    private final PublicPqrsMapper publicPqrsMapper;

    private final PublicResponseMapper publicResponseMapper;

    private OficinaMapper oficinaMapper;

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
        PublicResponseMapper publicResponseMapper
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

    public Optional<PqrsDTO> findOneOficina(String id) {
        LOG.debug("Request to get Pqrs : {}", id);
        return pqrsRepository
            .findById(id)
            .map(pqrs -> {
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

        ZoneId zoneSystem = ZoneId.systemDefault();
        LocalDateTime currentDate = LocalDateTime.ofInstant(globalCurrentDate, zoneSystem);
        LocalDateTime dueDate = currentDate.plusDays(15);
        pqrs.setFechaLimiteRespuesta(dueDate);

        Oficina office = oficinaRepository.findByNombre("Secretaría General");
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

        if (
            PqrsStatus.RESOLVED.getDisplayName().equalsIgnoreCase(pqrs.getEstado()) &&
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.FUNCTIONARY)
        ) {
            Pqrs oldPqrs = pqrsRepository.findById(pqrs.getId()).orElse(null);

            boolean changeState = !Objects.equals(oldPqrs.getEstado(), pqrs.getEstado());

            if (changeState) {
                pqrsNotificationService.sendPqrsNotification(pqrs, PqrsNotificationType.PQRS_RESOLVED, AuthoritiesConstants.FRONT_DESK_CS);
            }
        }

        if (pqrsDTO.getArchivosAdjuntosDTO() != null) {
            Set<String> archivosAdjuntosIds = pqrsDTO
                .getArchivosAdjuntosDTO()
                .stream()
                .map(ArchivoAdjuntoDTO::getId)
                .collect(Collectors.toSet());

            Set<ArchivoAdjunto> archivosAdjuntos = new HashSet<>(attachedFileRepository.findAllById(archivosAdjuntosIds));
            pqrs.setArchivosAdjuntos(archivosAdjuntos);
        }

        pqrs = pqrsRepository.save(pqrs);
        return pqrsMapper.toDto(pqrs);
    }

    public PublicPqrsDTO createPublicPqrs(PublicPqrsDTO publicPqrsDTO) {
        LOG.debug("Request to create public Pqrs : {}", publicPqrsDTO);
        Pqrs pqrs = publicPqrsMapper.toEntity(publicPqrsDTO);

        pqrs.setAccessToken(UUID.randomUUID().toString());
        pqrs.setFechaCreacion(Instant.now());
        if (pqrs.getEstado() == null) {
            pqrs.setEstado(PqrsStatus.PENDING.getDisplayName());
        }

        Pqrs savedPqrs = pqrsRepository.save(pqrs);

        Set<ArchivoAdjunto> successfullyLinkedAttachments = new HashSet<>();

        if (publicPqrsDTO.get_transientAttachments() != null && !publicPqrsDTO.get_transientAttachments().isEmpty()) {
            for (ArchivoAdjuntoDTO adjuntoDTO : publicPqrsDTO.get_transientAttachments()) {
                if (adjuntoDTO.getId() != null) {
                    Optional<ArchivoAdjunto> adjuntoOpt = attachedFileRepository.findById(adjuntoDTO.getId());
                    if (adjuntoOpt.isPresent()) {
                        ArchivoAdjunto adjuntoToLink = adjuntoOpt.get();
                        adjuntoToLink.setPqrsAttachment(savedPqrs);
                        attachedFileRepository.save(adjuntoToLink);
                        successfullyLinkedAttachments.add(adjuntoToLink);
                    } else {
                        LOG.warn(
                            "ArchivoAdjunto with ID {} provided in DTO not found. Cannot link to PQRS {}.",
                            adjuntoDTO.getId(),
                            savedPqrs.getId()
                        );
                    }
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

        return publicPqrsMapper.toDto(savedPqrs);
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
        response.setFechaRespuesta(publicResponseDTO.getFechaRespuesta());
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
}
