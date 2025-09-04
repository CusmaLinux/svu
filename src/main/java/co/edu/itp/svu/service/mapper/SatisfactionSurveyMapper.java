package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.SatisfactionSurvey;
import co.edu.itp.svu.repository.SatisfactionSurveyRepository;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.SatisfactionSurveyDTO;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper for the entity {@link SatisfactionSurvey} and its DTO {@link SatisfactionSurveyDTO}.
 */
@Mapper(componentModel = "spring")
public abstract class SatisfactionSurveyMapper implements EntityMapper<SatisfactionSurveyDTO, SatisfactionSurvey> {

    @Autowired
    protected SatisfactionSurveyRepository satisfactionSurveyRepository;

    @Override
    @Mapping(target = "pqrs", ignore = true)
    public abstract SatisfactionSurveyDTO toDto(SatisfactionSurvey satisfactionSurvey);

    @AfterMapping
    protected void populatePqrsDto(SatisfactionSurvey satisfactionSurvey, @MappingTarget SatisfactionSurveyDTO satisfactionSurveyDTO) {
        if (satisfactionSurvey.getPqrs() != null) {
            PqrsDTO pqrsDTO = new PqrsDTO();
            pqrsDTO.setId(satisfactionSurvey.getPqrs().getId());
            satisfactionSurveyDTO.setPqrs(pqrsDTO);
        }
    }

    @Override
    @Mapping(target = "pqrs", ignore = true)
    public abstract SatisfactionSurvey toEntity(SatisfactionSurveyDTO satisfactionSurveyDTO);

    @AfterMapping
    protected void linkPqrs(SatisfactionSurveyDTO satisfactionSurveyDTO, @MappingTarget SatisfactionSurvey satisfactionSurvey) {
        if (satisfactionSurveyDTO.getPqrs() != null && satisfactionSurveyDTO.getPqrs().getId() != null) {
            Pqrs pqrs = new Pqrs();
            pqrs.setId(satisfactionSurveyDTO.getPqrs().getId());
            satisfactionSurvey.setPqrs(pqrs);
        }
    }

    public SatisfactionSurvey fromId(String id) {
        if (id == null) {
            return null;
        }
        return satisfactionSurveyRepository.findById(id).orElse(null);
    }
}
