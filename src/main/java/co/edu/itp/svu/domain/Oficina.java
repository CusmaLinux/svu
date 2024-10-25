package co.edu.itp.svu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * A Oficina.
 */
@Document(collection = "oficina")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Oficina implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("nombre")
    private String nombre;

    @Field("descripcion")
    private String descripcion;

    @NotNull
    @Field("nivel")
    private String nivel;

    @Field("oficina_superior")
    private String oficinaSuperior;

    // Usamos @DBRef si quieres hacer referencia a la entidad User en la colección de MongoDB
    @DBRef
    @Field("responsable")
    private User responsable;

    @DBRef
    @Field("notificacions")
    @JsonIgnoreProperties(value = { "destinatarios" }, allowSetters = true)
    private Set<Notificacion> notificacions = new HashSet<>();

    // Este campo almacenará las PQRS dirigidas a esta oficina
    @DBRef
    @Field("pqrsList")
    private List<Pqrs> pqrsList;

    public List<Pqrs> getPqrsList() {
        return pqrsList;
    }

    public void setPqrsList(List<Pqrs> pqrsList) {
        this.pqrsList = pqrsList;
    }

    public void addPqrs(Pqrs pqrs) {
        this.pqrsList.add(pqrs);
    }

    public User getResponsable() {
        return responsable;
    }

    public void setResponsable(User responsable) {
        this.responsable = responsable;
    }

    // jhipster-needle-entity-add-field - JHipster will add fields here
    // El responsable es un usuario que tendrá un rol específico

    public Oficina(String nombre, User responsable) {
        this.nombre = nombre;
        this.responsable = responsable;
        this.pqrsList = new ArrayList<>();
    }

    public Oficina() {
        this.nombre = "";
        this.responsable = null;
        this.pqrsList = new ArrayList<>();
    }

    public String getId() {
        return this.id;
    }

    public Oficina id(String id) {
        this.setId(id);
        return this;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return this.nombre;
    }

    public Oficina nombre(String nombre) {
        this.setNombre(nombre);
        return this;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return this.descripcion;
    }

    public Oficina descripcion(String descripcion) {
        this.setDescripcion(descripcion);
        return this;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNivel() {
        return this.nivel;
    }

    public Oficina nivel(String nivel) {
        this.setNivel(nivel);
        return this;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }

    public String getOficinaSuperior() {
        return this.oficinaSuperior;
    }

    public Oficina oficinaSuperior(String oficinaSuperior) {
        this.setOficinaSuperior(oficinaSuperior);
        return this;
    }

    public void setOficinaSuperior(String oficinaSuperior) {
        this.oficinaSuperior = oficinaSuperior;
    }

    public Set<Notificacion> getNotificacions() {
        return this.notificacions;
    }

    public void setNotificacions(Set<Notificacion> notificacions) {
        if (this.notificacions != null) {
            this.notificacions.forEach(i -> i.removeDestinatarios(this));
        }
        if (notificacions != null) {
            notificacions.forEach(i -> i.addDestinatarios(this));
        }
        this.notificacions = notificacions;
    }

    public Oficina notificacions(Set<Notificacion> notificacions) {
        this.setNotificacions(notificacions);
        return this;
    }

    public Oficina addNotificacion(Notificacion notificacion) {
        this.notificacions.add(notificacion);
        notificacion.getDestinatarios().add(this);
        return this;
    }

    public Oficina removeNotificacion(Notificacion notificacion) {
        this.notificacions.remove(notificacion);
        notificacion.getDestinatarios().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Oficina)) {
            return false;
        }
        return getId() != null && getId().equals(((Oficina) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Oficina{" +
            "id=" + getId() +
            ", nombre='" + getNombre() + "'" +
            ", descripcion='" + getDescripcion() + "'" +
            ", nivel='" + getNivel() + "'" +
            ", oficinaSuperior='" + getOficinaSuperior() + "'" +
            "}";
    }
}
