package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.InformePqrs;
import co.edu.itp.svu.domain.dto.InformPqrsCounts;
import co.edu.itp.svu.repository.InformePqrsRepository;
import co.edu.itp.svu.service.dto.InformePqrsDTO;
import co.edu.itp.svu.service.mapper.InformePqrsMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing
 * {@link co.edu.itp.svu.domain.InformePqrs}.
 */
@Service
public class InformePqrsService {

    private static final Logger LOG = LoggerFactory.getLogger(InformePqrsService.class);

    private final InformePqrsRepository informePqrsRepository;

    private final InformePqrsMapper informePqrsMapper;

    public InformePqrsService(InformePqrsRepository informePqrsRepository, InformePqrsMapper informePqrsMapper) {
        this.informePqrsRepository = informePqrsRepository;
        this.informePqrsMapper = informePqrsMapper;
    }

    /**
     * Save a informPqrs.
     *
     * @param informePqrsDTO the entity to save.
     * @return the persisted entity.
     */
    public InformePqrsDTO save(InformePqrsDTO informePqrsDTO) {
        LOG.debug("Request to save InformePqrs : {}", informePqrsDTO);
        InformePqrs informPqrs = informePqrsMapper.toEntity(informePqrsDTO);

        String oficinaId = (informPqrs.getOficina() != null) ? informPqrs.getOficina().getId() : null;

        InformPqrsCounts counts = informePqrsRepository
            .calculatePqrsTotals(informPqrs.getFechaInicio(), informPqrs.getFechaFin(), oficinaId)
            .orElse(new InformPqrsCounts());

        informPqrs.setTotalPqrs(counts.getTotalPqrs() != null ? counts.getTotalPqrs() : 0);
        informPqrs.setTotalResueltas(counts.getTotalResueltas() != null ? counts.getTotalResueltas() : 0);
        informPqrs.setTotalPendientes(counts.getTotalPendientes() != null ? counts.getTotalPendientes() : 0);

        informPqrs = informePqrsRepository.save(informPqrs);
        return informePqrsMapper.toDto(informPqrs);
    }

    /**
     * Update a informPqrs.
     *
     * @param informePqrsDTO the entity to save.
     * @return the persisted entity.
     */
    public InformePqrsDTO update(InformePqrsDTO informePqrsDTO) {
        LOG.debug("Request to update InformePqrs : {}", informePqrsDTO);
        InformePqrs informPqrs = informePqrsMapper.toEntity(informePqrsDTO);
        informPqrs = informePqrsRepository.save(informPqrs);
        return informePqrsMapper.toDto(informPqrs);
    }

    /**
     * Partially update a informPqrs.
     *
     * @param informePqrsDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<InformePqrsDTO> partialUpdate(InformePqrsDTO informePqrsDTO) {
        LOG.debug("Request to partially update InformePqrs : {}", informePqrsDTO);

        return informePqrsRepository
            .findById(informePqrsDTO.getId())
            .map(existingInformePqrs -> {
                informePqrsMapper.partialUpdate(existingInformePqrs, informePqrsDTO);

                return existingInformePqrs;
            })
            .map(informePqrsRepository::save)
            .map(informePqrsMapper::toDto);
    }

    /**
     * Get all the informPqrs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<InformePqrsDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all InformePqrs");
        return informePqrsRepository.findAll(pageable).map(informePqrsMapper::toDto);
    }

    /**
     * Get one informPqrs by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<InformePqrsDTO> findOne(String id) {
        LOG.debug("Request to get InformePqrs : {}", id);
        return informePqrsRepository.findById(id).map(informePqrsMapper::toDto);
    }

    /**
     * Delete the informPqrs by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete InformePqrs : {}", id);
        informePqrsRepository.deleteById(id);
    }
}
