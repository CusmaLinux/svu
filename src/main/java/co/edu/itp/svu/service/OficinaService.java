package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.OfficeCases;
import co.edu.itp.svu.service.dto.OfficeCases.CaseReference;
import co.edu.itp.svu.service.dto.OfficeOption;
import co.edu.itp.svu.service.dto.OficinaDTO;
import co.edu.itp.svu.service.mapper.OficinaMapper;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link co.edu.itp.svu.domain.Oficina}.
 */
@Service
public class OficinaService {

    private static final Logger LOG = LoggerFactory.getLogger(OficinaService.class);

    private final PqrsAuthorizationService authorization;

    private final UserService userService;

    private final OficinaRepository oficinaRepository;

    private final PqrsRepository pqrsRepository;

    private final OficinaMapper oficinaMapper;

    public OficinaService(
        OficinaRepository oficinaRepository,
        OficinaMapper oficinaMapper,
        UserService userService,
        PqrsRepository pqrsRepository,
        PqrsAuthorizationService authorization
    ) {
        this.oficinaRepository = oficinaRepository;
        this.oficinaMapper = oficinaMapper;
        this.authorization = authorization;
        this.userService = userService;
        this.pqrsRepository = pqrsRepository;
    }

    @Transactional(readOnly = true)
    public Page<OficinaDTO> search(String criteria, Pageable pageable) {
        return oficinaRepository.search(criteria, pageable).map(oficinaMapper::toDto);
    }

    /**
     * Save a oficina.
     *
     * @param oficinaDTO the entity to save.
     * @return the persisted entity.
     */
    public OficinaDTO save(OficinaDTO oficinaDTO) {
        Oficina oficina = oficinaMapper.toEntity(oficinaDTO);
        oficina = oficinaRepository.save(oficina);
        return oficinaMapper.toDto(oficina);
    }

    /**
     * Update a oficina.
     *
     * @param oficinaDTO the entity to save.
     * @return the persisted entity.
     */
    public OficinaDTO update(OficinaDTO oficinaDTO) {
        Oficina oficina = oficinaMapper.toEntity(oficinaDTO);
        oficina = oficinaRepository.save(oficina);
        return oficinaMapper.toDto(oficina);
    }

    /**
     * Partially update a oficina.
     *
     * @param oficinaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OficinaDTO> partialUpdate(OficinaDTO oficinaDTO) {
        return oficinaRepository
            .findById(oficinaDTO.getId())
            .map(existingOficina -> {
                oficinaMapper.partialUpdate(existingOficina, oficinaDTO);

                return existingOficina;
            })
            .map(oficinaRepository::save)
            .map(oficinaMapper::toDto);
    }

    /**
     * Get all the oficinas.
     *
     * @return the list of entities.
     */
    public Page<OficinaDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Oficinas");
        return oficinaRepository.findAll(pageable).map(oficinaMapper::toDto);
    }

    /**
     * Get one oficina by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<OficinaDTO> findOne(String id) {
        LOG.debug("Request to get Oficina : {}", id);
        return oficinaRepository.findById(id).map(oficinaMapper::toDto);
    }

    /**
     * Delete the oficina by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete Oficina : {}", id);
        oficinaRepository.deleteById(id);
    }

    ///////////////// modificaciones////////////////////////////////////////7

    public OficinaDTO createOficina(OficinaDTO oficinaDTO) {
        // Validación inicial del DTO
        if (oficinaDTO == null) {
            LOG.error("OficinaDTO no puede ser null");
            throw new IllegalArgumentException("OficinaDTO no puede ser null");
        }

        // Validar campos obligatorios
        if (oficinaDTO.getNombre() == null || oficinaDTO.getNombre().isEmpty()) {
            LOG.error("El nombre de la oficina es requerido");
            throw new IllegalArgumentException("El nombre de la oficina es requerido");
        }

        if (oficinaDTO.getResponsableDTO() == null || oficinaDTO.getResponsableDTO().getLogin() == null) {
            LOG.error("El usuario responsable es requerido");
            throw new IllegalArgumentException("El usuario responsable es requerido");
        }

        String usernameResponsable = oficinaDTO.getResponsableDTO().getLogin();
        Optional<User> responsable = userService.getUserWithAuthoritiesByLogin(usernameResponsable);

        if (!responsable.isPresent()) {
            throw new IllegalArgumentException("El usuario responsable no existe");
        }

        // Mapeo del DTO a la entidad
        Oficina oficina;
        try {
            oficina = oficinaMapper.toEntity(oficinaDTO);
            if (oficina == null) {
                LOG.error("Error al mapear OficinaDTO a Oficina");
                throw new IllegalArgumentException("Error al mapear OficinaDTO a Oficina");
            }

            oficina.setResponsable(responsable.orElseThrow()); // Asociar el User como responsable
            oficina = oficinaRepository.save(oficina);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar la oficina", e);
        }

        return oficinaMapper.toDto(oficina);
    }

    // Actualizar una oficina existente un usuario responsable
    public OficinaDTO updateOficina(OficinaDTO oficinaDTO) {
        Oficina oficina = oficinaMapper.toEntity(oficinaDTO);
        Optional<Oficina> oficinaOptional = oficinaRepository.findById(oficinaDTO.getId());
        if (oficinaOptional.isPresent()) {
            String usernameResponsable = oficinaDTO.getResponsableDTO().getLogin();
            oficina = oficinaOptional.orElseThrow();
            oficina.setNombre(oficinaDTO.getNombre());
            oficina.setOficinaSuperior(oficinaDTO.getOficinaSuperior());
            oficina.setDescripcion(oficinaDTO.getDescripcion());
            oficina.setNivel(oficinaDTO.getNivel());
            Optional<User> responsable = userService.getUserWithAuthoritiesByLogin(usernameResponsable);
            if (responsable.isPresent()) {
                oficina.setResponsable(responsable.orElseThrow());
                oficina = oficinaRepository.save(oficina);
                return oficinaMapper.toDto(oficina);
            } else {
                throw new IllegalArgumentException("El usuario responsable no existe");
            }
        } else {
            throw new IllegalArgumentException("La oficina no existe");
        }
    }

    public Page<OfficeOption> catalog(Pageable pageable) {
        return oficinaRepository.findOptions(pageable).map(office -> new OfficeOption(office.id(), office.nombre()));
    }

    public Optional<OfficeCases> getOficina(String id) {
        if (id.isBlank()) return Optional.empty();
        String officeId = authorization.requireOfficeFilter(id).orElseThrow();
        return oficinaRepository
            .findOptionById(officeId)
            .map(office -> {
                List<CaseReference> references = pqrsRepository
                    .findReferencesByOficinaResponder_Id(office.id())
                    .stream()
                    .map(reference -> new CaseReference(reference.id(), reference.fileNumber()))
                    .toList();
                return new OfficeCases(office.id(), office.nombre(), references);
            });
    }
}
