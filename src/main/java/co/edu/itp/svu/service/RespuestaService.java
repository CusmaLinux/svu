package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.ArchivoAdjuntoRepository;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.AuthoritiesConstants;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.ResponseDTO;
import co.edu.itp.svu.service.mapper.ResponseMapper;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link co.edu.itp.svu.domain.Respuesta}.
 */
@Service
public class RespuestaService {

    private static final Logger LOG = LoggerFactory.getLogger(RespuestaService.class);

    private final RespuestaRepository respuestaRepository;

    private final ArchivoAdjuntoRepository attachedFileRepository;

    private final ResponseMapper responseMapper;

    private final UserRepository userRepository;

    private final OficinaRepository oficinaRepository;

    public RespuestaService(
        RespuestaRepository respuestaRepository,
        ResponseMapper responseMapper,
        UserRepository userRepository,
        ArchivoAdjuntoRepository attachedFileRepository,
        OficinaRepository oficinaRepository
    ) {
        this.respuestaRepository = respuestaRepository;
        this.responseMapper = responseMapper;
        this.userRepository = userRepository;
        this.attachedFileRepository = attachedFileRepository;
        this.oficinaRepository = oficinaRepository;
    }

    @Transactional(readOnly = true)
    public Page<ResponseDTO> search(String criteria, Pageable pageable) {
        LOG.debug("Request to search for a page of responses or query {}", criteria);

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

        return respuestaRepository.search(criteria, officeId, pageable).map(responseMapper::toDto);
    }

    /**
     * Save a response.
     *
     * @param respuestaDTO the entity to save.
     * @return the persisted entity.
     */
    public ResponseDTO save(ResponseDTO responseDTO) {
        LOG.debug("Request to save Respuesta : {}", responseDTO);

        String currentUserLogin = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new IllegalStateException("Current user login not found"));

        User currentUser = userRepository
            .findOneByLogin(currentUserLogin)
            .orElseThrow(() -> new IllegalStateException("User with login '" + currentUserLogin + "' not found"));

        Respuesta response = responseMapper.toEntity(responseDTO);

        response.setFechaRespuesta(Instant.now());
        response.setResolver(currentUser);

        Respuesta resultResponse = respuestaRepository.save(response);

        Set<ArchivoAdjunto> successfullyLinkedAttachments = new HashSet<>();

        if (responseDTO.get_transientAttachments() != null && !responseDTO.get_transientAttachments().isEmpty()) {
            for (ArchivoAdjuntoDTO adjuntoDTO : responseDTO.get_transientAttachments()) {
                if (adjuntoDTO.getId() != null) {
                    Optional<ArchivoAdjunto> adjuntoOpt = attachedFileRepository.findById(adjuntoDTO.getId());
                    adjuntoOpt.ifPresentOrElse(
                        adjuntoToLink -> {
                            adjuntoToLink.setResponseAttachment(resultResponse);
                            attachedFileRepository.save(adjuntoToLink);
                            successfullyLinkedAttachments.add(adjuntoToLink);
                        },
                        () ->
                            LOG.warn(
                                "ArchivoAdjunto with ID {} provided in DTO not found. Cannot link to PQRS {}.",
                                adjuntoDTO.getId(),
                                resultResponse.getId()
                            )
                    );
                } else {
                    LOG.warn("ArchivoAdjuntoDTO in _transientAttachments is missing an ID. Cannot link.");
                }
            }
        }

        if (!successfullyLinkedAttachments.isEmpty()) {
            resultResponse.set_transientAttachments(successfullyLinkedAttachments);
        } else {
            resultResponse.set_transientAttachments(new HashSet<>());
        }

        return responseMapper.toDto(response);
    }

    /**
     * Update a response.
     *
     * @param respuestaDTO the entity to save.
     * @return the persisted entity.
     */
    public ResponseDTO update(ResponseDTO respuestaDTO) {
        LOG.debug("Request to update Respuesta : {}", respuestaDTO);
        Respuesta response = responseMapper.toEntity(respuestaDTO);
        response = respuestaRepository.save(response);
        return responseMapper.toDto(response);
    }

    /**
     * Partially update a response.
     *
     * @param respuestaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ResponseDTO> partialUpdate(ResponseDTO respuestaDTO) {
        LOG.debug("Request to partially update Respuesta : {}", respuestaDTO);

        return respuestaRepository
            .findById(respuestaDTO.getId())
            .map(existingRespuesta -> {
                responseMapper.partialUpdate(existingRespuesta, respuestaDTO);

                return existingRespuesta;
            })
            .map(respuestaRepository::save)
            .map(responseMapper::toDto);
    }

    /**
     * Get all the respuestas.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<ResponseDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Respuestas");
        return respuestaRepository.findAll(pageable).map(responseMapper::toDto);
    }

    /**
     * Get one response by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<ResponseDTO> findOne(String id) {
        LOG.debug("Request to get Respuesta : {}", id);
        return respuestaRepository.findById(id).map(responseMapper::toDto);
    }

    /**
     * Delete the response by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete Respuesta : {}", id);
        respuestaRepository.deleteById(id);
    }
}
