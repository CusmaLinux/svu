package co.edu.itp.svu.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.security.AuthoritiesConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression coverage for role-based access to PQRSD case endpoints.
 * Office-assignment fixtures are needed for cross-office integration tests.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PqrsOfficeSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotListInstitutionalCases() throws Exception {
        mockMvc.perform(get("/api/pqrs")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotSearchCases() throws Exception {
        mockMvc.perform(get("/api/pqrs/search")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotDeleteCase() throws Exception {
        mockMvc.perform(delete("/api/pqrs/nonexistent-case")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.FUNCTIONARY)
    void functionaryCannotReassignCase() throws Exception {
        mockMvc.perform(put("/api/pqrs/nonexistent-case/office").param("officeName", "Other office"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotReadCaseDetail() throws Exception {
        mockMvc.perform(get("/api/pqrs/nonexistent-case")).andExpect(status().isForbidden());
    }
}
