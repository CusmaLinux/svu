package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.Respuesta;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.repository.RespuestaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
@AutoConfigureMockMvc
class ResponseSecurityIT {

    private static final String API = "/api/respuestas";
    private static final Instant CREATED = Instant.parse("2025-01-01T00:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    RespuestaRepository responses;

    @Autowired
    ObjectMapper json;

    final List<Object> fixtures = new ArrayList<>();
    User localUser;
    User remoteUser;
    User withoutOffice;
    Oficina remoteOffice;
    Pqrs localCase;
    Pqrs remoteCase;
    Pqrs unassigned;
    Respuesta local;
    Respuesta remote;
    Respuesta unassignedResponse;
    String prefix;

    @BeforeEach
    void setup() {
        prefix = "test-" + UUID.randomUUID();
        localUser = operator();
        remoteUser = operator();
        withoutOffice = operator();
        Oficina localOffice = persist(new Oficina(prefix + "-A", localUser).nivel("1"));
        remoteOffice = persist(new Oficina(prefix + "-B", remoteUser).nivel("1"));
        localCase = newCase(localOffice, "A");
        remoteCase = newCase(remoteOffice, "B");
        unassigned = newCase(null, "C");
        local = newResponse(localCase);
        remote = newResponse(remoteCase);
        unassignedResponse = newResponse(unassigned);
    }

    @AfterEach
    void cleanup() {
        for (Pqrs pqrs : fixtures.stream().filter(Pqrs.class::isInstance).map(Pqrs.class::cast).toList()) {
            responses.deleteAll(responses.findByPqrsId(pqrs.getId()));
        }
        Collections.reverse(fixtures);
        fixtures.forEach(mongo::remove);
    }

    private <T> T persist(T value) {
        T saved = mongo.save(value);
        fixtures.add(saved);
        return saved;
    }

    private User operator() {
        User user = new User();
        user.setLogin("test-" + UUID.randomUUID());
        user.setPassword("x".repeat(60));
        return persist(user);
    }

    private Pqrs newCase(Oficina office, String suffix) {
        Pqrs pqrs = new Pqrs().type(PqrsType.REQUEST).titulo("Caso sintético").estado("PENDIENTE").oficinaResponder(office);
        pqrs.setFileNumber(prefix + suffix);
        pqrs.setAccessToken(UUID.randomUUID().toString());
        return persist(pqrs);
    }

    private Respuesta newResponse(Pqrs pqrs) {
        Respuesta response = new Respuesta().pqrs(pqrs).contenido("Original").fechaRespuesta(CREATED);
        response.setResolver(localUser);
        return persist(response);
    }

    private RequestPostProcessor as(User user, String... roles) {
        return user(user.getLogin()).authorities(AuthorityUtils.createAuthorityList(roles));
    }

    private String creation(Pqrs pqrs) throws Exception {
        return json.writeValueAsString(Map.of("contenido", "Nueva", "pqrs", Map.of("id", pqrs.getId())));
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, FRONT_DESK_CS })
    void institutionalReadIncludesUnassignedCases(String role) throws Exception {
        mvc.perform(get(API + "/{id}", unassignedResponse.getId()).with(as(withoutOffice, role))).andExpect(status().isOk());
        mvc
            .perform(get(API + "/search").param("query", prefix).param("size", "1").with(as(withoutOffice, role)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "3"))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void functionaryListAndSearchUseSameOfficeBeforePagination(boolean reports) throws Exception {
        var actor = reports ? as(localUser, FUNCTIONARY, PQRSD_REPORTS) : as(localUser, FUNCTIONARY);
        for (String path : List.of(API, API + "/search")) {
            mvc
                .perform(get(path).param("query", prefix).param("size", "1").with(actor))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].id").value(local.getId()));
            mvc
                .perform(get(path).param("query", prefix).param("size", "1").param("page", "1").with(actor))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$", hasSize(0)));
        }
        mvc.perform(get(API + "/{id}", remote.getId()).with(actor)).andExpect(status().isForbidden());
        mvc.perform(get(API + "/{id}", unassignedResponse.getId()).with(actor)).andExpect(status().isForbidden());
        mvc.perform(get(API + "/{id}", local.getId()).with(actor)).andExpect(status().isOk());
        mvc
            .perform(get(API + "/search").param("query", ".*").with(actor))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "0"));
        mvc
            .perform(post(API).with(actor).contentType(MediaType.APPLICATION_JSON).content(creation(localCase)))
            .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, PQRSD_REPORTS, FUNCTIONARY })
    void missingOperationalScopeDeniesReads(String role) throws Exception {
        for (String path : List.of(API, API + "/search", API + "/" + local.getId())) {
            mvc.perform(get(path).with(as(withoutOffice, role))).andExpect(status().isForbidden());
        }
    }

    @Test
    void ambiguousOfficeDeniesReadAndCreate() throws Exception {
        persist(new Oficina(prefix + "-duplicate", localUser).nivel("1"));
        missingOperationalScopeDeniesReadsFor(localUser);
        deniedCreate(localUser, localCase, FUNCTIONARY);
    }

    private void missingOperationalScopeDeniesReadsFor(User user) throws Exception {
        mvc.perform(get(API).with(as(user, FUNCTIONARY))).andExpect(status().isForbidden());
        mvc.perform(get(API + "/{id}", local.getId()).with(as(user, FUNCTIONARY))).andExpect(status().isForbidden());
    }

    private void deniedCreate(User user, Pqrs pqrs, String... roles) throws Exception {
        long before = responses.count();
        mvc
            .perform(post(API).with(as(user, roles)).contentType(MediaType.APPLICATION_JSON).content(creation(pqrs)))
            .andExpect(status().isForbidden());
        assertThat(responses.count()).isEqualTo(before);
    }

    @Test
    void creationUsesAuthenticatedAuthorAndPersistedCase() throws Exception {
        var payload = json.writeValueAsString(
            Map.of(
                "contenido",
                "Nueva",
                "pqrs",
                Map.of("id", localCase.getId()),
                "resolver",
                Map.of("id", remoteUser.getId()),
                "fechaRespuesta",
                CREATED.toString()
            )
        );
        mvc
            .perform(post(API).with(as(localUser, FUNCTIONARY, PQRSD_REPORTS)).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.resolver.id").value(localUser.getId()))
            .andExpect(jsonPath("$.pqrs.id").value(localCase.getId()))
            .andExpect(jsonPath("$.fechaRespuesta", not(is(CREATED.toString()))));
        mvc
            .perform(
                post(API).with(as(withoutOffice, ADMIN, FUNCTIONARY)).contentType(MediaType.APPLICATION_JSON).content(creation(unassigned))
            )
            .andExpect(status().isCreated());
    }

    @Test
    void creationCannotBorrowInstitutionalReadScope() throws Exception {
        deniedCreate(localUser, remoteCase, FUNCTIONARY, FRONT_DESK_CS);
        deniedCreate(localUser, unassigned, FUNCTIONARY);
        deniedCreate(localUser, localCase, FRONT_DESK_CS);
        deniedCreate(withoutOffice, localCase, FUNCTIONARY, PQRSD_REPORTS);
        deniedCreate(withoutOffice, localCase, USER);
        deniedCreate(withoutOffice, localCase, PQRSD_REPORTS);
        Pqrs missing = new Pqrs();
        missing.setId("missing-" + prefix);
        deniedCreate(withoutOffice, missing, PQRSD_REPORTS);
        mvc
            .perform(
                post(API)
                    .with(as(localUser, FRONT_DESK_CS, FUNCTIONARY))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(creation(localCase))
            )
            .andExpect(status().isCreated());
    }

    @Test
    void anonymousCanOnlyUseTheCitizenRoutes() throws Exception {
        mvc.perform(get(API)).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/search")).andExpect(status().isUnauthorized());
        mvc.perform(get(API + "/{id}", local.getId())).andExpect(status().isUnauthorized());
        long before = responses.count();
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(creation(localCase))).andExpect(status().isUnauthorized());
        assertThat(responses.count()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = { FRONT_DESK_CS, FUNCTIONARY, USER, PQRSD_REPORTS })
    void editsAndDeletesRequireAdministration(String role) throws Exception {
        String data = json.writeValueAsString(Map.of("id", local.getId(), "contenido", "No permitido"));
        var actor = as(localUser, role);
        mvc
            .perform(put(API + "/{id}", local.getId()).with(actor).contentType(MediaType.APPLICATION_JSON).content(data))
            .andExpect(status().isForbidden());
        mvc
            .perform(patch(API + "/{id}", local.getId()).with(actor).contentType(MediaType.APPLICATION_JSON).content(data))
            .andExpect(status().isForbidden());
        mvc.perform(delete(API + "/{id}", local.getId()).with(actor)).andExpect(status().isForbidden());
        assertOriginal();
    }

    @ParameterizedTest
    @ValueSource(strings = { "pqrs", "resolver", "fechaRespuesta" })
    void evenAdministrationCannotChangeIdentity(String field) throws Exception {
        Object replacement =
            switch (field) {
                case "pqrs" -> Map.of("id", remoteCase.getId());
                case "resolver" -> Map.of("id", remoteUser.getId());
                default -> Instant.EPOCH.toString();
            };
        String data = json.writeValueAsString(Map.of("id", local.getId(), "contenido", "No permitido", field, replacement));
        var actor = as(withoutOffice, ADMIN);
        mvc
            .perform(put(API + "/{id}", local.getId()).with(actor).contentType(MediaType.APPLICATION_JSON).content(data))
            .andExpect(status().isForbidden());
        mvc
            .perform(patch(API + "/{id}", local.getId()).with(actor).contentType(MediaType.APPLICATION_JSON).content(data))
            .andExpect(status().isForbidden());
        assertOriginal();
    }

    private void assertOriginal() {
        Respuesta persisted = responses.findById(local.getId()).orElseThrow();
        assertThat(persisted.getContenido()).isEqualTo("Original");
        assertThat(persisted.getPqrs().getId()).isEqualTo(localCase.getId());
        assertThat(persisted.getResolver().getId()).isEqualTo(localUser.getId());
        assertThat(persisted.getFechaRespuesta()).isEqualTo(CREATED);
    }

    private ArchivoAdjunto attachmentFor(Respuesta response) {
        ArchivoAdjunto file = new ArchivoAdjunto().nombre("fixture.txt").tipo("text/plain").fechaSubida(CREATED);
        file.setResponseAttachment(response);
        return file;
    }

    @Test
    void updateRetainsOwnAttachmentButCannotClaimAnotherOrAnOrphan() throws Exception {
        String own = persist(attachmentFor(local)).getId();
        String foreign = persist(attachmentFor(remote)).getId();
        String orphan = persist(new ArchivoAdjunto().nombre("fixture.txt").tipo("text/plain").fechaSubida(CREATED)).getId();
        for (String id : List.of(foreign, orphan)) {
            var refs = List.of(Map.of("id", id));
            String data = json.writeValueAsString(Map.of("id", local.getId(), "contenido", "No permitido", "_transientAttachments", refs));
            mvc
                .perform(
                    patch(API + "/{id}", local.getId()).with(as(localUser, ADMIN)).contentType(MediaType.APPLICATION_JSON).content(data)
                )
                .andExpect(status().isForbidden());
            long before = responses.count();
            mvc
                .perform(
                    post(API)
                        .with(as(localUser, ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsString(
                                Map.of("pqrs", Map.of("id", localCase.getId()), "contenido", "No permitido", "_transientAttachments", refs)
                            )
                        )
                )
                .andExpect(status().isForbidden());
            assertThat(responses.count()).isEqualTo(before);
        }
        assertOriginal();
        assertThat(mongo.findById(foreign, ArchivoAdjunto.class).getResponseAttachment().getId()).isEqualTo(remote.getId());
        assertThat(mongo.findById(orphan, ArchivoAdjunto.class).getResponseAttachment()).isNull();
        String valid = json.writeValueAsString(
            Map.of("id", local.getId(), "contenido", "Editada", "_transientAttachments", List.of(Map.of("id", own)))
        );
        mvc
            .perform(put(API + "/{id}", local.getId()).with(as(localUser, ADMIN)).contentType(MediaType.APPLICATION_JSON).content(valid))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido").value("Editada"))
            .andExpect(jsonPath("$._transientAttachments[0].id").value(own));
    }

    @Test
    void reassignedCaseFollowsCurrentOfficeAndPublicTokenStillWorks() throws Exception {
        localCase.setOficinaResponder(remoteOffice);
        mongo.save(localCase);
        mvc.perform(get(API + "/{id}", local.getId()).with(as(localUser, FUNCTIONARY))).andExpect(status().isForbidden());
        deniedCreate(localUser, localCase, FUNCTIONARY);
        mvc.perform(get(API + "/{id}", local.getId()).with(as(remoteUser, FUNCTIONARY))).andExpect(status().isOk());
        var publicResponse = new MockMultipartFile(
            "response",
            "",
            "application/json",
            json.writeValueAsBytes(Map.of("contenido", "Ciudadana"))
        );
        mvc
            .perform(multipart("/api/public/pqrs/{token}/responses", localCase.getAccessToken()).file(publicResponse))
            .andExpect(status().isCreated());
        mvc
            .perform(get("/api/public/pqrs/{token}", localCase.getAccessToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._transientResponses[*].contenido", hasItems("Original", "Ciudadana")));
    }
}
