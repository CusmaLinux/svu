package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.NonBusinessDay;
import co.edu.itp.svu.service.dto.NonBusinessDayDTO;
import org.mapstruct.Mapper;

/**
 * Mapper for the entity {@link NonBusinessDay} and its DTO {@link NonBusinessDayDTO}.
 */
@Mapper(componentModel = "spring")
public interface NonBusinessDayMapper extends EntityMapper<NonBusinessDayDTO, NonBusinessDay> {}
