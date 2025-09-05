package co.edu.itp.svu.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

/**
 * A DTO for the {@link co.edu.itp.svu.domain.SatisfactionSurvey} entity.
 */
public class SatisfactionSurveyDTO implements Serializable {

    private String id;

    @NotNull
    @Min(value = 1)
    @Max(value = 5)
    private Integer rating;

    private String comment;

    private Instant submissionDate;

    private PqrsDTO pqrs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Instant getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(Instant submissionDate) {
        this.submissionDate = submissionDate;
    }

    public PqrsDTO getPqrs() {
        return pqrs;
    }

    public void setPqrs(PqrsDTO pqrs) {
        this.pqrs = pqrs;
    }
}
