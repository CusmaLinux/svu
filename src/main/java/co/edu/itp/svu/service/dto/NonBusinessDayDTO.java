package co.edu.itp.svu.service.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.NonBusinessDay} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NonBusinessDayDTO implements Serializable {

    private String id;

    @NotNull
    private LocalDate date;

    @NotNull
    private String description;

    public NonBusinessDayDTO() {
        // Empty constructor needed for Jackson.
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NonBusinessDayDTO)) {
            return false;
        }

        NonBusinessDayDTO nonBusinessDayDTO = (NonBusinessDayDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, nonBusinessDayDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NonBusinessDayDTO{" +
            "id='" + getId() + "'" +
            ", date='" + getDate() + "'" +
            ", description='" + getDescription() + "'" +
            "}";
    }
}
