package co.edu.itp.svu.service.dto;

// This class is used to map the results from the MongoDB aggregation pipeline.
public class InformPqrsCounts {

    private Integer totalPqrs;
    private Integer totalResueltas;
    private Integer totalPendientes;

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
}
