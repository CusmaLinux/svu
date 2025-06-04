package co.edu.itp.svu.service.dto.api;

import co.edu.itp.svu.service.dto.ArchivoAdjuntoDTO;
import co.edu.itp.svu.service.dto.UserDTO;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class PublicResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String contenido;

    @NotNull
    private Instant fechaRespuesta;

    private UserDTO resolver;

    private boolean byRequester;

    private Set<ArchivoAdjuntoDTO> _transientAttachments = new HashSet<>();

    public PublicResponseDTO() {}

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

    public void setFechaRespuesta(Instant fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public UserDTO getResolver() {
        return resolver;
    }

    public void setResolver(UserDTO resolver) {
        this.resolver = resolver;
    }

    public boolean isByRequester() {
        return byRequester;
    }

    public void setByRequester(boolean byRequester) {
        this.byRequester = byRequester;
    }

    public Set<ArchivoAdjuntoDTO> get_transientAttachments() {
        return _transientAttachments;
    }

    public void set_transientAttachments(Set<ArchivoAdjuntoDTO> _transientAttachments) {
        this._transientAttachments = _transientAttachments;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicResponseDTO that = (PublicResponseDTO) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    @Override
    public String toString() {
        return (
            "PublicResponseDTO{" +
            "id='" +
            id +
            '\'' +
            ", byRequester=" +
            byRequester +
            ", resolverLogin=" +
            (resolver != null ? resolver.getLogin() : "N/A") +
            ", _transientAttachmentsCount=" +
            (_transientAttachments != null ? _transientAttachments.size() : 0) +
            '}'
        );
    }
}
