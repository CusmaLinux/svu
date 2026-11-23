package co.edu.itp.svu.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.security.AuthoritiesConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies report downloads are restricted before any report lookup takes place.
 * A missing report ID is intentional: unauthorized requests must be rejected
 * by the security filter rather than reaching the report generator.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ReportResourceSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithAnonymousUser
    void anonymousUserCannotDownloadReport() throws Exception {
        mockMvc.perform(get("/api/reports/pqrs/missing-report/download")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotDownloadReport() throws Exception {
        mockMvc.perform(get("/api/reports/pqrs/missing-report/download")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.FUNCTIONARY)
    void functionaryCannotDownloadReportWithoutReportingAuthority() throws Exception {
        mockMvc.perform(get("/api/reports/pqrs/missing-report/download")).andExpect(status().isForbidden());
    }
}
