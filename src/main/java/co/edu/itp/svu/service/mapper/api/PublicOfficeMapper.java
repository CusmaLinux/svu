package co.edu.itp.svu.service.mapper.api;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.service.dto.api.PublicOfficeDTO;
import co.edu.itp.svu.service.mapper.EntityMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface PublicOfficeMapper extends EntityMapper<PublicOfficeDTO, Oficina> {
    @Override
    PublicOfficeDTO toDto(Oficina oficina);

    @Override
    @Mapping(target = "nivel", ignore = true)
    @Mapping(target = "oficinaSuperior", ignore = true)
    @Mapping(target = "pqrsList", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    Oficina toEntity(PublicOfficeDTO publicOficinaDTO);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "nivel", ignore = true)
    @Mapping(target = "oficinaSuperior", ignore = true)
    @Mapping(target = "pqrsList", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    void partialUpdate(@MappingTarget Oficina oficina, PublicOfficeDTO publicOficinaDTO);
}
