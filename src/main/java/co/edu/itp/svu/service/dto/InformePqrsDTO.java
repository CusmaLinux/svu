package co.edu.itp.svu.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.InformePqrs} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class InformePqrsDTO implements Serializable {

    private String id;

    @NotNull
    private Instant fechaInicio;

    @NotNull
    private Instant fechaFin;

    private Integer totalPqrs;

    private Integer totalResueltas;

    private Integer totalPendientes;

    private OficinaDTO oficina;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Instant fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Instant getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Instant fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getTotalPqrs() {
        return totalPqrs;
    }

    public void setTotalPqrs(Integer totalPqrs) {
        this.totalPqrs = totalPqrs;
    }

    public Integer getTotalResueltas() {
        return totalResueltas;
    }

    public void setTotalResueltas(Integer totalResueltas) {
        this.totalResueltas = totalResueltas;
    }

    public Integer getTotalPendientes() {
        return totalPendientes;
    }

    public void setTotalPendientes(Integer totalPendientes) {
        this.totalPendientes = totalPendientes;
    }

    public OficinaDTO getOficina() {
        return oficina;
    }

    public void setOficina(OficinaDTO oficina) {
        this.oficina = oficina;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InformePqrsDTO)) {
            return false;
        }

        InformePqrsDTO informePqrsDTO = (InformePqrsDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, informePqrsDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "InformePqrsDTO{" +
            "id='" + getId() + "'" +
            ", fechaInicio='" + getFechaInicio() + "'" +
            ", fechaFin='" + getFechaFin() + "'" +
            ", totalPqrs=" + getTotalPqrs() +
            ", totalResueltas=" + getTotalResueltas() +
            ", totalPendientes=" + getTotalPendientes() +
            ", oficina=" + getOficina() +
            "}";
    }
}
