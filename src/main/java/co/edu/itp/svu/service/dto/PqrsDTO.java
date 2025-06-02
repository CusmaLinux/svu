package co.edu.itp.svu.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.Pqrs} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PqrsDTO implements Serializable {

    private String id;

    @NotNull
    private String titulo;

    private String descripcion;

    private String requesterEmail;

    private String accessToken;

    private Integer daysToReply;

    private Instant fechaCreacion;

    private LocalDateTime fechaLimiteRespuesta;

    private String estado;

    private OficinaDTO oficinaResponder;

    private Set<ArchivoAdjuntoDTO> archivosAdjuntosDTO;

    public Set<ArchivoAdjuntoDTO> getArchivosAdjuntosDTO() {
        return archivosAdjuntosDTO;
    }

    public void setArchivosAdjuntosDTO(Set<ArchivoAdjuntoDTO> archivosAdjuntos) {
        this.archivosAdjuntosDTO = archivosAdjuntos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getDaysToReply() {
        return daysToReply;
    }

    public void setDaysToReply(Integer daysToReply) {
        this.daysToReply = daysToReply;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaLimiteRespuesta() {
        return fechaLimiteRespuesta;
    }

    public void setFechaLimiteRespuesta(LocalDateTime fechaLimiteRespuesta) {
        this.fechaLimiteRespuesta = fechaLimiteRespuesta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public OficinaDTO getOficinaResponder() {
        return oficinaResponder;
    }

    public void setOficinaResponder(OficinaDTO oficinaResponder) {
        this.oficinaResponder = oficinaResponder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PqrsDTO)) {
            return false;
        }

        PqrsDTO pqrsDTO = (PqrsDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pqrsDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PqrsDTO{" +
                "id='" + getId() + "'" +
                ", titulo='" + getTitulo() + "'" +
                ", descripcion='" + getDescripcion() + "'" +
                ", requesterEmail='" + getRequesterEmail() + "'" +
                ", accessToken='" + (getAccessToken() != null ? "******" : "null") + "'" +
                ", daysToReply='" + getDaysToReply() + "'" +
                ", fechaCreacion='" + getFechaCreacion() + "'" +
                ", fechaLimiteRespuesta='" + getFechaLimiteRespuesta() + "'" +
                ", estado='" + getEstado() + "'" +
                ", oficinaResponder=" + getOficinaResponder() +
                "}";
    }
}
