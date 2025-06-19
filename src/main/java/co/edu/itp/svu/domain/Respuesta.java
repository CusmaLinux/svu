package co.edu.itp.svu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A Respuesta.
 */
@Document(collection = "respuesta")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Respuesta implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("contenido")
    private String contenido;

    @Field("fecha_respuesta")
    private Instant fechaRespuesta;

    @DBRef
    @Field("resolver_user")
    private User resolver;

    @DBRef
    @Field("pqrs")
    @JsonIgnoreProperties(value = { "archivosAdjuntos", "oficinaResponder" }, allowSetters = true)
    private Pqrs pqrs;

    @Transient
    private transient Set<ArchivoAdjunto> _transientAttachments;

    public Set<ArchivoAdjunto> get_transientAttachments() {
        return _transientAttachments;
    }

    public void set_transientAttachments(Set<ArchivoAdjunto> attachments) {
        this._transientAttachments = attachments;
    }

    public String getId() {
        return this.id;
    }

    public Respuesta id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContenido() {
        return this.contenido;
    }

    public Respuesta contenido(String contenido) {
        this.setContenido(contenido);
        return this;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Instant getFechaRespuesta() {
        return this.fechaRespuesta;
    }

    public Respuesta fechaRespuesta(Instant fechaRespuesta) {
        this.setFechaRespuesta(fechaRespuesta);
        return this;
    }

    public void setFechaRespuesta(Instant fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public User getResolver() {
        return resolver;
    }

    public void setResolver(User resolver) {
        this.resolver = resolver;
    }

    public Pqrs getPqrs() {
        return this.pqrs;
    }

    public void setPqrs(Pqrs pqrs) {
        this.pqrs = pqrs;
    }

    public Respuesta pqrs(Pqrs pqrs) {
        this.setPqrs(pqrs);
        return this;
    }

    @Transient
    public boolean isByRequester() {
        return this.resolver == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Respuesta)) {
            return false;
        }
        return getId() != null && getId().equals(((Respuesta) o).getId());
    }

    @Override
    public int hashCode() {
        // see
        // https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        User resolverUser = getResolver();
        String resolverString;
        if (resolverUser != null) {
            resolverString = "User{id='" + resolverUser.getId() + "', login='" + resolverUser.getLogin() + "'}";
        } else {
            resolverString = "null";
        }

        return "Respuesta{" +
                "id=" + getId() +
                ", contenido='" + getContenido() + "'" +
                ", fechaRespuesta='" + getFechaRespuesta() + "'" +
                ", resolver=" + resolverString +
                ", isByRequester=" + isByRequester() +
                "}";
    }
}
