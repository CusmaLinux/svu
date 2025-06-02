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

    private PqrsDTO pqr;

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

    public Instant getFechaRespuesta() {
        return fechaRespuesta;
    }

    public PqrsDTO getPqr() {
        return pqr;
    }

    public void setPqr(PqrsDTO pqr) {
        this.pqr = pqr;
    }

    public User getResolver() {
        return resolver;
    }

    public void setResolver(User resolver) {
        this.resolver = resolver;
    }

    public void setFechaRespuesta(Instant fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
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
                ", pqr=" + getPqr() +
                "}";
    }
}
