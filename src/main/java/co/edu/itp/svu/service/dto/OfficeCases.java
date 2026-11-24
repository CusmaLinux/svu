package co.edu.itp.svu.service.dto;

import java.util.List;

public record OfficeCases(String id, String nombre, List<CaseReference> pqrsList) {
    public record CaseReference(String id, String fileNumber) {}
}
