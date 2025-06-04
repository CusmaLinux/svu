package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.RespuestaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Respuesta} and its DTO {@link RespuestaDTO}.
 */
@Mapper(componentModel = "spring")
public interface RespuestaMapper extends EntityMapper<RespuestaDTO, Respuesta> {
    @Mapping(target = "pqrs", source = "pqrs", qualifiedByName = "pqrsId")
    RespuestaDTO toDto(Respuesta s);

    @Named("pqrsId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PqrsDTO toDtoPqrsId(Pqrs pqrs);
}
