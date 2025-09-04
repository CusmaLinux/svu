package co.edu.itp.svu.domain;

import co.edu.itp.svu.domain.enumeration.PqrsType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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

    @Field("file_number")
    private String fileNumber;

    @NotNull
    @Field("type")
    private PqrsType type;

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
    private Instant fechaLimiteRespuesta;

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

    @DBRef
    @Field("satisfaction_survey")
    @JsonIgnoreProperties(value = { "pqrs" }, allowSetters = true)
    private SatisfactionSurvey satisfactionSurvey;

    @Transient
    private transient Set<ArchivoAdjunto> _transientAttachments;

    @Transient
    private transient Set<Respuesta> _transientResponses;

    public SatisfactionSurvey getSatisfactionSurvey() {
        return satisfactionSurvey;
    }

    public void setSatisfactionSurvey(SatisfactionSurvey satisfactionSurvey) {
        this.satisfactionSurvey = satisfactionSurvey;
    }

    public Set<ArchivoAdjunto> get_transientAttachments() {
        return _transientAttachments;
    }

    public void set_transientAttachments(Set<ArchivoAdjunto> attachments) {
        this._transientAttachments = attachments;
    }

    public Set<Respuesta> get_transientResponses() {
        return _transientResponses;
    }

    public void set_transientResponses(Set<Respuesta> responses) {
        this._transientResponses = responses;
    }

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

    public Instant getFechaLimiteRespuesta() {
        return this.fechaLimiteRespuesta;
    }

    public Pqrs fechaLimiteRespuesta(Instant fechaLimiteRespuesta) {
        this.setFechaLimiteRespuesta(fechaLimiteRespuesta);
        return this;
    }

    public void setFechaLimiteRespuesta(Instant fechaLimiteRespuesta) {
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
        return this;
    }

    public Pqrs removeArchivosAdjuntos(ArchivoAdjunto archivoAdjunto) {
        this.archivosAdjuntos.remove(archivoAdjunto);
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
                ", type='" + getType() + "'" +
                ", fileNumber='" + getFileNumber() + "'" +
                ", titulo='" + getTitulo() + "'" +
                ", descripcion='" + getDescripcion() + "'" +
                ", requesterEmail='" + getRequesterEmail() + "'" +
                ", accessToken='" + (getAccessToken() != null ? "******" : "null") + "'" +
                ", daysToReply='" + getDaysToReply() + "'" +
                ", fechaCreacion='" + getFechaCreacion() + "'" +
                ", fechaLimiteRespuesta='" + getFechaLimiteRespuesta() + "'" +
                ", estado='" + getEstado() + "'" +
                "}";
    }
}
