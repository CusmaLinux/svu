package co.edu.itp.svu.service.dto.api;

import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A DTO to handle public PQRS for the {@link co.edu.itp.svu.domain.Pqrs} entity
 */
public class PublicPqrsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileNumber;

    @NotNull
    private PqrsType type;

    @NotNull
    private String titulo;

    private String descripcion;

    private Instant fechaCreacion;

    private Instant fechaLimiteRespuesta;

    private String estado;

    // Fields specific to anonymous/public context
    private String requesterEmail;

    private String accessToken;

    // Associated data
    private PublicOfficeDTO oficinaResponder;

    private Set<ArchivoAdjuntoDTO> _transientAttachments = new HashSet<>();

    private Set<PublicResponseDTO> _transientResponses = new HashSet<>();

    public PublicPqrsDTO() {}

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public PqrsType getType() {
        return type;
    }

    public void setType(PqrsType type) {
        this.type = type;
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

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaLimiteRespuesta() {
        return fechaLimiteRespuesta;
    }

    public void setFechaLimiteRespuesta(Instant fechaLimiteRespuesta) {
        this.fechaLimiteRespuesta = fechaLimiteRespuesta;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
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

    public PublicOfficeDTO getoficinaResponder() {
        return oficinaResponder;
    }

    public void setoficinaResponder(PublicOfficeDTO oficinaResponder) {
        this.oficinaResponder = oficinaResponder;
    }

    public Set<ArchivoAdjuntoDTO> get_transientAttachments() {
        return _transientAttachments;
    }

    public void set_transientAttachments(Set<ArchivoAdjuntoDTO> _transientAttachments) {
        this._transientAttachments = _transientAttachments;
    }

    public Set<PublicResponseDTO> get_transientResponses() {
        return _transientResponses;
    }

    public void set_transientResponses(Set<PublicResponseDTO> _transientResponses) {
        this._transientResponses = _transientResponses;
    }

    // equals, hashCode, toString (recommended for DTOs)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicPqrsDTO that = (PublicPqrsDTO) o;
        return java.util.Objects.equals(accessToken, that.accessToken); // Typically ID is enough for DTO equality
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(accessToken);
    }

    @Override
    public String toString() {
        return (
            "PublicPqrsDTO{" +
            '\'' +
            "type='" +
            type +
            '\'' +
            "fileNumber='" +
            fileNumber +
            '\'' +
            ", titulo='" +
            titulo +
            '\'' +
            ", estado='" +
            estado +
            '\'' +
            ", requesterEmail='" +
            requesterEmail +
            '\'' +
            ", accessToken='" +
            (accessToken != null ? "******" : "null") +
            '\'' +
            ", _transientAttachmentsCount=" +
            (_transientAttachments != null ? _transientAttachments.size() : 0) +
            ", _transientResponsesCount=" +
            (_transientResponses != null ? _transientResponses.size() : 0) +
            '}'
        );
    }
}
