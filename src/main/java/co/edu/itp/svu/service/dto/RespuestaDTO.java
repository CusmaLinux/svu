package co.edu.itp.svu.service.dto;

import co.edu.itp.svu.domain.User;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.Respuesta} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RespuestaDTO implements Serializable {

    private String id;

    private String contenido;

    @NotNull
    private Instant fechaRespuesta;

    private PqrsDTO pqrs;

    private User resolver;

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

    public User getResolver() {
        return resolver;
    }

    public void setResolver(User resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RespuestaDTO)) {
            return false;
        }

        RespuestaDTO respuestaDTO = (RespuestaDTO) o;
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
        return "RespuestaDTO{" +
                "id='" + getId() + "'" +
                ", contenido='" + getContenido() + "'" +
                ", fechaRespuesta='" + getFechaRespuesta() + "'" +
                ", resolver='" + getResolver().getId() + "'" +
                ", pqrs=" + getPqrs() +
                "}";
    }
}
