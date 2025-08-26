package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.NonBusinessDay;
import co.edu.itp.svu.repository.NonBusinessDayRepository;
import co.edu.itp.svu.service.dto.NonBusinessDayDTO;
import co.edu.itp.svu.service.mapper.NonBusinessDayMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service Implementation for managing {@link co.edu.itp.svu.domain.NonBusinessDay}.
 */
@Service
public class NonBusinessDayService {

    private final Logger log = LoggerFactory.getLogger(NonBusinessDayService.class);

    private final NonBusinessDayRepository nonBusinessDayRepository;

    private final NonBusinessDayMapper nonBusinessDayMapper;

    public NonBusinessDayService(NonBusinessDayRepository nonBusinessDayRepository, NonBusinessDayMapper nonBusinessDayMapper) {
        this.nonBusinessDayRepository = nonBusinessDayRepository;
        this.nonBusinessDayMapper = nonBusinessDayMapper;
    }

    /**
     * Save a nonBusinessDay.
     *
     * @param nonBusinessDayDTO the entity to save.
     * @return the persisted entity.
     */
    public NonBusinessDayDTO save(NonBusinessDayDTO nonBusinessDayDTO) {
        log.debug("Request to save NonBusinessDay : {}", nonBusinessDayDTO);
        NonBusinessDay nonBusinessDay = nonBusinessDayMapper.toEntity(nonBusinessDayDTO);
        nonBusinessDay = nonBusinessDayRepository.save(nonBusinessDay);
        return nonBusinessDayMapper.toDto(nonBusinessDay);
    }

    /**
     * Update a nonBusinessDay.
     *
     * @param nonBusinessDayDTO the entity to save.
     * @return the persisted entity.
     */
    public NonBusinessDayDTO update(NonBusinessDayDTO nonBusinessDayDTO) {
        log.debug("Request to update NonBusinessDay : {}", nonBusinessDayDTO);
        NonBusinessDay nonBusinessDay = nonBusinessDayMapper.toEntity(nonBusinessDayDTO);
        nonBusinessDay = nonBusinessDayRepository.save(nonBusinessDay);
        return nonBusinessDayMapper.toDto(nonBusinessDay);
    }

    /**
     * Get all the nonBusinessDays.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    public Page<NonBusinessDayDTO> findAll(Pageable pageable) {
        log.debug("Request to get all NonBusinessDays");
        return nonBusinessDayRepository.findAll(pageable).map(nonBusinessDayMapper::toDto);
    }

    /**
     * Get one nonBusinessDay by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public Optional<NonBusinessDayDTO> findOne(String id) {
        log.debug("Request to get NonBusinessDay : {}", id);
        return nonBusinessDayRepository.findById(id).map(nonBusinessDayMapper::toDto);
    }

    /**
     * Delete the nonBusinessDay by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete NonBusinessDay : {}", id);
        nonBusinessDayRepository.deleteById(id);
    }
}
