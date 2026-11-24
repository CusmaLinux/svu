package co.edu.itp.svu.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.security.AuthoritiesConstants;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de integración para verificar la seguridad
 * y el control de acceso a las PQRSD.
 *
 * Se comprueba:
 *
 * 1. Restricciones para usuarios ordinarios.
 * 2. Restricciones para funcionarios.
 * 3. Acceso a detalles de PQRSD.
 * 4. Restricciones de reasignación.
 * 5. Restricciones de eliminación.
 * 6. Aislamiento de información entre oficinas.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PqrsOfficeSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Un usuario ordinario no puede listar
     * las PQRSD institucionales.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotListInstitutionalCases() throws Exception {
        mockMvc.perform(get("/api/pqrs")).andExpect(status().isForbidden());
    }

    /**
     * Un usuario ordinario no puede utilizar
     * la búsqueda institucional de PQRSD.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotSearchCases() throws Exception {
        mockMvc.perform(get("/api/pqrs/search")).andExpect(status().isForbidden());
    }

    /**
     * Un usuario ordinario no puede eliminar
     * una PQRSD.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotDeleteCase() throws Exception {
        mockMvc.perform(delete("/api/pqrs/nonexistent-case")).andExpect(status().isForbidden());
    }

    /**
     * Un funcionario no puede reasignar
     * una PQRSD a otra oficina.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.FUNCTIONARY)
    void functionaryCannotReassignCase() throws Exception {
        mockMvc.perform(put("/api/pqrs/nonexistent-case/office").param("officeName", "Other office")).andExpect(status().isForbidden());
    }

    /**
     * Un funcionario no puede eliminar
     * una PQRSD.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.FUNCTIONARY)
    void functionaryCannotDeleteCase() throws Exception {
        mockMvc.perform(delete("/api/pqrs/nonexistent-case")).andExpect(status().isForbidden());
    }

    /**
     * Un usuario ordinario no puede reasignar
     * una PQRSD.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotReassignCase() throws Exception {
        mockMvc.perform(put("/api/pqrs/nonexistent-case/office").param("officeName", "Other office")).andExpect(status().isForbidden());
    }

    /**
     * Un usuario ordinario no puede consultar
     * el detalle de una PQRSD existente.
     *
     * Se crea una PQRSD real para evitar
     * que el resultado sea 404 por inexistencia.
     */
    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void ordinaryUserCannotReadCaseDetail() throws Exception {
        Pqrs pqrs = new Pqrs().type(PqrsType.REQUEST).titulo("Prueba seguridad " + UUID.randomUUID()).estado("PENDIENTE");

        try {
            mongoTemplate.save(pqrs);

            mockMvc.perform(get("/api/pqrs/{id}", pqrs.getId())).andExpect(status().isForbidden());
        } finally {
            if (pqrs.getId() != null) {
                mongoTemplate.remove(pqrs);
            }
        }
    }

    /**
     * Un funcionario no puede consultar
     * una PQRSD asignada a otra oficina.
     *
     * Escenario:
     *
     * Funcionario -> Oficina A
     * PQRSD        -> Oficina B
     *
     * Resultados esperados: HTTP 200 para su propia oficina y HTTP 403 para otra oficina.
     */

    @Test
    @WithMockUser(username = "funcionario.prueba.svu", authorities = AuthoritiesConstants.FUNCTIONARY)
    void functionaryCanOnlyReadCasesFromOwnOffice() throws Exception {
        String marker = UUID.randomUUID().toString();

        User funcionario = new User();
        funcionario.setLogin("funcionario.prueba.svu");
        funcionario.setPassword("x".repeat(60));
        funcionario.setActivated(true);
        funcionario.setEmail("funcionario." + marker + "@example.com");

        Oficina oficinaA = new Oficina();
        oficinaA.setNombre("Oficina A " + marker);
        oficinaA.setNivel("1");

        Oficina oficinaB = new Oficina();
        oficinaB.setNombre("Oficina B " + marker);
        oficinaB.setNivel("1");

        Pqrs pqrsPropia = new Pqrs().type(PqrsType.REQUEST).titulo("PQRS propia " + marker).estado("PENDIENTE");

        Pqrs pqrsAjena = new Pqrs().type(PqrsType.REQUEST).titulo("PQRS ajena " + marker).estado("PENDIENTE");

        try {
            mongoTemplate.save(funcionario);

            oficinaA.setResponsable(funcionario);

            mongoTemplate.save(oficinaA);
            mongoTemplate.save(oficinaB);

            pqrsPropia.setOficinaResponder(oficinaA);
            pqrsAjena.setOficinaResponder(oficinaB);

            mongoTemplate.save(pqrsPropia);
            mongoTemplate.save(pqrsAjena);

            // Debe permitir consultar su propia oficina
            mockMvc.perform(get("/api/pqrs/{id}", pqrsPropia.getId())).andExpect(status().isOk());

            // Debe impedir consultar otra oficina
            mockMvc.perform(get("/api/pqrs/{id}", pqrsAjena.getId())).andExpect(status().isForbidden());
        } finally {
            if (pqrsPropia.getId() != null) {
                mongoTemplate.remove(pqrsPropia);
            }

            if (pqrsAjena.getId() != null) {
                mongoTemplate.remove(pqrsAjena);
            }

            if (oficinaB.getId() != null) {
                mongoTemplate.remove(oficinaB);
            }

            if (oficinaA.getId() != null) {
                mongoTemplate.remove(oficinaA);
            }

            if (funcionario.getId() != null) {
                mongoTemplate.remove(funcionario);
            }
        }
    }
}
