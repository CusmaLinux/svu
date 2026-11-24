package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.ArchivoAdjuntoRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.repository.RespuestaRepository;
import co.edu.itp.svu.repository.UserRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.security.SecurityUtils;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.ResponseDTO;
import co.edu.itp.svu.service.errors.BadRequestAlertException;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.ResponseMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class RespuestaService {

    private final RespuestaRepository respuestaRepository;
    private final PqrsRepository pqrsRepository;
    private final ArchivoAdjuntoRepository attachedFileRepository;
    private final ResponseMapper responseMapper;
    private final ArchivoAdjuntoMapper attachedFileMapper;
    private final UserRepository userRepository;
    private final PqrsAuthorizationService authorization;

    public RespuestaService(
        RespuestaRepository respuestaRepository,
        PqrsRepository pqrsRepository,
        ArchivoAdjuntoRepository attachedFileRepository,
        ResponseMapper responseMapper,
        ArchivoAdjuntoMapper attachedFileMapper,
        UserRepository userRepository,
        PqrsAuthorizationService authorization
    ) {
        this.respuestaRepository = respuestaRepository;
        this.pqrsRepository = pqrsRepository;
        this.attachedFileRepository = attachedFileRepository;
        this.responseMapper = responseMapper;
        this.attachedFileMapper = attachedFileMapper;
        this.userRepository = userRepository;
        this.authorization = authorization;
    }

    public Page<ResponseDTO> search(String criteria, Pageable pageable) {
        return respuestaRepository.search(criteria, authorization.requireOperationalOffice(), pageable).map(responseMapper::toDto);
    }

    public Page<ResponseDTO> findAll(Pageable pageable) {
        return search("", pageable);
    }

    public Optional<ResponseDTO> findOne(String id) {
        Optional<String> office = authorization.requireOperationalOffice();
        return respuestaRepository
            .findById(id)
            .map(response -> {
                Pqrs pqrs = persistedCase(response);
                authorization.checkCaseInAuthorizedOffice(pqrs, office);
                return toDetail(response, pqrs);
            });
    }

    public ResponseDTO save(ResponseDTO input) {
        Optional<String> office = authorization.requireResponseCreationOffice();
        User author = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .orElseThrow(() -> new AccessDeniedException("Unknown response author"));
        Pqrs pqrs = findCase(input.getPqrs() == null ? null : input.getPqrs().getId()).orElseThrow(() ->
            new BadRequestAlertException("Select an existing PQRSD", "respuesta", "pqrsrequired")
        );
        authorization.checkCaseInAuthorizedOffice(pqrs, office);
        validateAttachments(input, null, pqrs);
        Respuesta response = new Respuesta().contenido(input.getContenido()).fechaRespuesta(Instant.now()).pqrs(pqrs);
        response.setResolver(author);
        return responseMapper.toDto(respuestaRepository.insert(response));
    }

    public ResponseDTO update(ResponseDTO input, boolean partial) {
        authorization.requireResponseManagementAccess();
        Respuesta response = respuestaRepository
            .findById(input.getId())
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", "respuesta", "idnotfound"));
        Pqrs pqrs = persistedCase(response);

        if (
            (input.getPqrs() != null && !Objects.equals(input.getPqrs().getId(), pqrs.getId())) ||
            (input.getResolver() != null &&
                (response.getResolver() == null || !Objects.equals(input.getResolver().getId(), response.getResolver().getId()))) ||
            (input.getFechaRespuesta() != null && !Objects.equals(input.getFechaRespuesta(), response.getFechaRespuesta()))
        ) {
            throw new AccessDeniedException("Cannot change response case, author or creation date");
        }

        validateAttachments(input, response.getId(), pqrs);
        if (!partial || input.getContenido() != null) {
            response.setContenido(input.getContenido());
        }
        return toDetail(respuestaRepository.save(response), pqrs);
    }

    public void delete(String id) {
        authorization.requireResponseManagementAccess();
        respuestaRepository
            .findById(id)
            .ifPresent(response -> {
                persistedCase(response);
                respuestaRepository.delete(response);
            });
    }

    private Optional<Pqrs> findCase(String id) {
        return id == null ? Optional.empty() : pqrsRepository.findById(id);
    }

    private Pqrs persistedCase(Respuesta response) {
        return findCase(response.getPqrs() == null ? null : response.getPqrs().getId()).orElseThrow(() ->
            new AccessDeniedException("Response case unavailable")
        );
    }

    private ResponseDTO toDetail(Respuesta response, Pqrs pqrs) {
        ResponseDTO dto = responseMapper.toDto(response);
        dto.set_transientAttachments(
            attachedFileRepository
                .findByResponseAttachment_Id(response.getId())
                .stream()
                .filter(file -> belongsToCase(file, pqrs))
                .map(attachedFileMapper::toDto)
                .collect(Collectors.toSet())
        );
        return dto;
    }

    private boolean belongsToCase(ArchivoAdjunto file, Pqrs pqrs) {
        return file.getPqrsAttachment() == null || Objects.equals(file.getPqrsAttachment().getId(), pqrs.getId());
    }

    private void validateAttachments(ResponseDTO input, String responseId, Pqrs pqrs) {
        Set<ArchivoAdjuntoDTO> attachments = input.get_transientAttachments();
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        // The existing upload records do not establish who may claim an unlinked file.
        if (responseId == null) {
            throw new AccessDeniedException("New attachment associations are unavailable");
        }
        Set<String> retainedIds = attachedFileRepository
            .findByResponseAttachment_Id(responseId)
            .stream()
            .filter(file -> belongsToCase(file, pqrs))
            .map(ArchivoAdjunto::getId)
            .collect(Collectors.toSet());
        if (attachments.stream().anyMatch(file -> file == null || !retainedIds.contains(file.getId()))) {
            throw new AccessDeniedException("Attachment is not associated with this response");
        }
    }
}
