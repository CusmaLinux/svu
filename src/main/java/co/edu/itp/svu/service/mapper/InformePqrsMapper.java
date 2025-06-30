package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.InformePqrs;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.service.dto.InformePqrsDTO;
import co.edu.itp.svu.service.dto.OficinaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link InformePqrs} and its DTO {@link InformePqrsDTO}.
 */
@Mapper(componentModel = "spring")
public interface InformePqrsMapper extends EntityMapper<InformePqrsDTO, InformePqrs> {
    @Mapping(target = "oficina", source = "oficina", qualifiedByName = "oficinaId")
    InformePqrsDTO toDto(InformePqrs s);

    @Named("oficinaId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    OficinaDTO toDtoOficinaId(Oficina oficina);
}
