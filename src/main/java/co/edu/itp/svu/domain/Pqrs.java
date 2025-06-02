package co.edu.itp.svu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A Pqrs.
 */
@Document(collection = "pqrs")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Pqrs implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("titulo")
    private String titulo;

    @Field("descripcion")
    private String descripcion;

    @Email
    @Size(min = 5, max = 254)
    @Indexed
    @Field("requester_email")
    private String requesterEmail;

    @Indexed(unique = true, sparse = true)
    @Field("access_token")
    private String accessToken;

    @Field("days_to_reply")
    private Integer daysToReply;

    @Field("fecha_creacion")
    private Instant fechaCreacion;

    @Field("fecha_limite_respuesta")
    private LocalDateTime fechaLimiteRespuesta;

    @NotNull
    @Field("estado")
    private String estado;

    @DBRef
    @Field("archivosAdjuntos")
    @JsonIgnoreProperties(value = { "pqrs" }, allowSetters = true)
    private Set<ArchivoAdjunto> archivosAdjuntos = new HashSet<>();

    @DBRef
    @Field("oficinaResponder")
    @JsonIgnoreProperties(value = { "notificacions" }, allowSetters = true)
    private Oficina oficinaResponder;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public String getId() {
        return this.id;
    }

    public Pqrs id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return this.titulo;
    }

    public Pqrs titulo(String titulo) {
        this.setTitulo(titulo);
        return this;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Pqrs descripcion(String descripcion) {
        this.setDescripcion(descripcion);
        return this;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Instant getFechaCreacion() {
        return this.fechaCreacion;
    }

    public Pqrs fechaCreacion(Instant fechaCreacion) {
        this.setFechaCreacion(fechaCreacion);
        return this;
    }

    public Integer getDaysToReply() {
        return daysToReply;
    }

    public void setDaysToReply(Integer daysToReply) {
        this.daysToReply = daysToReply;
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

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaLimiteRespuesta() {
        return this.fechaLimiteRespuesta;
    }

    public Pqrs fechaLimiteRespuesta(LocalDateTime fechaLimiteRespuesta) {
        this.setFechaLimiteRespuesta(fechaLimiteRespuesta);
        return this;
    }

    public void setFechaLimiteRespuesta(LocalDateTime fechaLimiteRespuesta) {
        this.fechaLimiteRespuesta = fechaLimiteRespuesta;
    }

    public String getEstado() {
        return this.estado;
    }

    public Pqrs estado(String estado) {
        this.setEstado(estado);
        return this;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Set<ArchivoAdjunto> getArchivosAdjuntos() {
        return this.archivosAdjuntos;
    }

    public void setArchivosAdjuntos(Set<ArchivoAdjunto> archivoAdjuntos) {
        this.archivosAdjuntos = archivoAdjuntos;
    }

    public Pqrs archivosAdjuntos(Set<ArchivoAdjunto> archivoAdjuntos) {
        this.setArchivosAdjuntos(archivoAdjuntos);
        return this;
    }

    public Pqrs addArchivosAdjuntos(ArchivoAdjunto archivoAdjunto) {
        this.archivosAdjuntos.add(archivoAdjunto);
        // archivoAdjunto.setPqrs(this);
        return this;
    }

    public Pqrs removeArchivosAdjuntos(ArchivoAdjunto archivoAdjunto) {
        this.archivosAdjuntos.remove(archivoAdjunto);
        // archivoAdjunto.setPqrs(null);
        return this;
    }

    public Oficina getOficinaResponder() {
        return this.oficinaResponder;
    }

    public void setOficinaResponder(Oficina oficina) {
        this.oficinaResponder = oficina;
    }

    public Pqrs oficinaResponder(Oficina oficina) {
        this.setOficinaResponder(oficina);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and
    // setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pqrs)) {
            return false;
        }
        return getId() != null && getId().equals(((Pqrs) o).getId());
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
        return "Pqrs{" +
                "id=" + getId() +
                ", titulo='" + getTitulo() + "'" +
                ", descripcion='" + getDescripcion() + "'" +
                ", solicitanteEmail='" + getRequesterEmail() + "'" +
                ", accessToken='" + (getAccessToken() != null ? "******" : "null")+ "'" +
                ", daysToReply='" + getDaysToReply() + "'" +
                ", fechaCreacion='" + getFechaCreacion() + "'" +
                ", fechaLimiteRespuesta='" + getFechaLimiteRespuesta() + "'" +
                ", estado='" + getEstado() + "'" +
                "}";
    }
}
