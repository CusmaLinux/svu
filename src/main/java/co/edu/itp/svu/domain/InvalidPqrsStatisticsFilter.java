package co.edu.itp.svu.domain;

public class InvalidPqrsStatisticsFilter extends IllegalArgumentException {

    private final String code;

    public InvalidPqrsStatisticsFilter(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
