package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.service.dto.PqrsDTO;
import co.edu.itp.svu.service.dto.ResponseDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Respuesta} and its DTO {@link ResponseDTO}.
 */
@Mapper(componentModel = "spring", uses = { PqrsMapper.class, UserMapper.class })
public interface ResponseMapper extends EntityMapper<ResponseDTO, Respuesta> {
    @Mapping(target = "pqrs", source = "pqrs", qualifiedByName = "pqrsWithFileNumber")
    @Mapping(target = "resolver", source = "resolver")
    ResponseDTO toDto(Respuesta s);

    @Override
    @Mapping(target = "_transientAttachments", ignore = true)
    @Mapping(target = "resolver", ignore = true)
    Respuesta toEntity(ResponseDTO responseDTO);

    @Override
    @Mapping(target = "_transientAttachments", ignore = true)
    @Mapping(target = "resolver", ignore = true)
    void partialUpdate(@MappingTarget Respuesta entity, ResponseDTO dto);

    @Named("pqrsId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    PqrsDTO toDtoPqrsId(Pqrs pqrs);

    @Named("pqrsWithFileNumber")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fileNumber", source = "fileNumber")
    PqrsDTO toDtoPqrsWithFileNumber(Pqrs pqrs);
}
