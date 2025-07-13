package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.service.dto.OficinaDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Oficina} and its DTO {@link OficinaDTO}.
 */
@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface OficinaMapper extends EntityMapper<OficinaDTO, Oficina> {
    @Override
    @Mapping(source = "responsable", target = "responsableDTO")
    OficinaDTO toDto(Oficina Oficina);

    @Override
    @Mapping(source = "responsableDTO", target = "responsable")
    Oficina toEntity(OficinaDTO OficinaDTO);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "responsableDTO", target = "responsable")
    void partialUpdate(@MappingTarget Oficina Oficina, OficinaDTO OficinaDTO);
}
