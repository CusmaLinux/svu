package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.Notificacion;
import co.edu.itp.svu.service.dto.NotificacionDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Notificacion} and its DTO {@link NotificacionDTO}.
 */
@Mapper(componentModel = "spring", uses = { UserMapper.class })
public interface NotificacionMapper extends EntityMapper<NotificacionDTO, Notificacion> {
    @Override
    // Source for 'recipient' is Notificacion entity
    @Mapping(source = "recipient", target = "recipientDto")
    NotificacionDTO toDto(Notificacion notificacion);

    @Override
    // Source for 'recipientDto' is NotificacionDTO
    @Mapping(source = "recipientDto", target = "recipient")
    Notificacion toEntity(NotificacionDTO notificacionDTO);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // Source for 'recipientDto' is NotificacionDTO (the second parameter)
    @Mapping(source = "recipientDto", target = "recipient")
    void partialUpdate(@MappingTarget Notificacion notificacion, NotificacionDTO notificacionDTO);
}
