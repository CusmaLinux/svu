package co.edu.itp.svu.service.mapper.api;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.service.dto.api.PublicPqrsDTO;
import co.edu.itp.svu.service.mapper.ArchivoAdjuntoMapper;
import co.edu.itp.svu.service.mapper.EntityMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = { PublicOfficeMapper.class, ArchivoAdjuntoMapper.class, PublicResponseMapper.class })
public interface PublicPqrsMapper extends EntityMapper<PublicPqrsDTO, Pqrs> {
    @Override
    @Mapping(source = "oficinaResponder", target = "oficinaResponder")
    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "requesterEmail", target = "requesterEmail")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(source = "_transientResponses", target = "_transientResponses")
    PublicPqrsDTO toDto(Pqrs pqrs);

    @Override
    @Mapping(source = "oficinaResponder", target = "oficinaResponder")
    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "requesterEmail", target = "requesterEmail")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(source = "_transientResponses", target = "_transientResponses")
    @Mapping(target = "archivosAdjuntos", ignore = true)
    @Mapping(target = "fechaLimiteRespuesta", ignore = true)
    @Mapping(target = "daysToReply", ignore = true)
    @Mapping(target = "removeArchivosAdjuntos", ignore = true)
    Pqrs toEntity(PublicPqrsDTO publicPqrsDTO);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "oficinaResponder", target = "oficinaResponder")
    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "requesterEmail", target = "requesterEmail")
    @Mapping(source = "_transientAttachments", target = "_transientAttachments")
    @Mapping(source = "_transientResponses", target = "_transientResponses")
    @Mapping(target = "archivosAdjuntos", ignore = true)
    @Mapping(target = "fechaLimiteRespuesta", ignore = true)
    @Mapping(target = "daysToReply", ignore = true)
    @Mapping(target = "removeArchivosAdjuntos", ignore = true)
    void partialUpdate(@MappingTarget Pqrs pqrs, PublicPqrsDTO publicPqrsDTO);
}
