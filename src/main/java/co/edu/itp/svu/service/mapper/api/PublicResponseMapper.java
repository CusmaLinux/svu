package co.edu.itp.svu.service.mapper.api;

import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.service.dto.api.PublicResponseDTO;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.EntityMapper;
import co.edu.itp.svu.service.mapper.UserMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = { UserMapper.class, ArchivoAdjuntoMapper.class })
public interface PublicResponseMapper extends EntityMapper<PublicResponseDTO, Respuesta> {
    @Override
    @Mapping(source = "resolver", target = "resolver", conditionExpression = "java(response.getResolver() != null)")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(target = "byRequester", ignore = true)
    PublicResponseDTO toDto(Respuesta response);

    @AfterMapping
    default void setDerivedFields(Respuesta response, @MappingTarget PublicResponseDTO dto) {
        if (response != null) {
            dto.setByRequester(response.isByRequester());
            if (response.isByRequester()) {
                dto.setResolver(null);
            }
        }
    }

    @Override
    @Mapping(source = "resolver", target = "resolver", conditionExpression = "java(publicResponseDTO.getResolver() != null)")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(target = "pqrs", ignore = true)
    Respuesta toEntity(PublicResponseDTO publicResponseDTO);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "resolver", target = "resolver", conditionExpression = "java(response.getResolver() != null)")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(target = "pqrs", ignore = true)
    void partialUpdate(@MappingTarget Respuesta response, PublicResponseDTO publicResponseDTO);
}
