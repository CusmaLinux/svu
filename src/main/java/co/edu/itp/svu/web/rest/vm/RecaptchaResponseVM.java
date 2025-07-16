package co.edu.itp.svu.web.rest.vm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecaptchaResponseVM {

    private final boolean success;
    private final float score;
    private final String action;
    private final String hostname;

    @JsonProperty("error-codes")
    private final List<String> errorCodes;

    @JsonCreator
    public RecaptchaResponseVM(
        @JsonProperty("success") boolean success,
        @JsonProperty("score") float score,
        @JsonProperty("action") String action,
        @JsonProperty("hostname") String hostname,
        @JsonProperty("error-codes") List<String> errorCodes
    ) {
        this.success = success;
        this.score = score;
        this.action = action;
        this.hostname = hostname;
        this.errorCodes = errorCodes;
    }

    public boolean isSuccess() {
        return success;
    }

    public float getScore() {
        return score;
    }

    public String getAction() {
        return action;
    }

    public String getHostname() {
        return hostname;
    }

    public List<String> getErrorCodes() {
        return errorCodes;
    }
}
