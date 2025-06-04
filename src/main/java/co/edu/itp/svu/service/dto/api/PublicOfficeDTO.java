package co.edu.itp.svu.service.dto.api;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * A DTO for the public data to {@link co.edu.itp.svu.domain.Oficina} entity.
 */
public class PublicOfficeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    @NotNull
    private String nombre;

    private String descripcion;

    public PublicOfficeDTO() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicOfficeDTO that = (PublicOfficeDTO) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PublicOficinaDTO{" + "id='" + id + '\'' + ", nombre='" + nombre + '\'' + '}';
    }
}
