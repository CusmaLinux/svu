package co.edu.itp.svu.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.Respuesta} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ResponseDTO implements Serializable {

    private String id;

    private String contenido;

    private Instant fechaRespuesta;

    private PqrsDTO pqrs;

    private UserDTO resolver;

    private Set<ArchivoAdjuntoDTO> _transientAttachments = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public void setFechaRespuesta(Instant fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public Instant getFechaRespuesta() {
        return fechaRespuesta;
    }

    public PqrsDTO getPqrs() {
        return pqrs;
    }

    public void setPqrs(PqrsDTO pqrs) {
        this.pqrs = pqrs;
    }

    public UserDTO getResolver() {
        return resolver;
    }

    public void setResolver(UserDTO resolver) {
        this.resolver = resolver;
    }

    public Set<ArchivoAdjuntoDTO> get_transientAttachments() {
        return _transientAttachments;
    }

    public void set_transientAttachments(Set<ArchivoAdjuntoDTO> _transientAttachments) {
        this._transientAttachments = _transientAttachments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResponseDTO)) {
            return false;
        }

        ResponseDTO respuestaDTO = (ResponseDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, respuestaDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        String resolverId = (getResolver() != null) ? getResolver().getId() : "null";

        return "RespuestaDTO{" +
                "id='" + getId() + "'" +
                ", contenido='" + getContenido() + "'" +
                ", fechaRespuesta='" + getFechaRespuesta() + "'" +
                ", resolver='" + resolverId + "'" +
                ", pqrs=" + getPqrs() +
                ", _transientAttachmentsCount=" +
                (_transientAttachments != null ? _transientAttachments.size() : 0) +
                "}";
    }
}
