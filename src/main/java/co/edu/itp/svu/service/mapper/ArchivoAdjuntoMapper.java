package co.edu.itp.svu.service.mapper;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ArchivoAdjuntoMapper extends EntityMapper<ArchivoAdjuntoDTO, ArchivoAdjunto> {
    ArchivoAdjuntoDTO toDto(ArchivoAdjunto s);

    ArchivoAdjunto toEntity(ArchivoAdjuntoDTO dto);
}
