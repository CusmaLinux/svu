package co.edu.itp.svu.web.rest;

import static co.edu.itp.svu.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.domain.*;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.service.notification.PqrsNotificationService;
import co.edu.itp.svu.service.notification.PqrsNotificationService.PqrsNotificationType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
@AutoConfigureMockMvc
class OfficeInboxSecurityIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    PqrsNotificationService delivery;

    User owner;
    User other;
    Oficina office;
    Oficina elsewhere;
    Pqrs local;
    Pqrs remote;
    Notificacion mine;
    Notificacion theirs;
    Notificacion orphan;

    @BeforeEach
    void fixtures() {
        owner = newUser();
        other = newUser();
        office = mongo.save(new Oficina("Oficina de prueba", owner).nivel("1"));
        elsewhere = mongo.save(new Oficina("Segunda oficina", null).nivel("1"));
        local = newCase(office);
        remote = newCase(elsewhere);
        mine = notice(owner);
        theirs = notice(other);
        orphan = notice(null);
    }

    User newUser() {
        User actor = new User();
        actor.setLogin("u-" + UUID.randomUUID());
        actor.setPassword("x".repeat(60));
        return mongo.save(actor);
    }

    Pqrs newCase(Oficina assigned) {
        return mongo.save(new Pqrs().type(PqrsType.REQUEST).titulo("sensitive-test-title").estado("PENDIENTE").oficinaResponder(assigned));
    }

    Notificacion notice(User recipient) {
        return mongo.save(
            new Notificacion()
                .recipient(recipient)
                .tipo("PQRS_CREATED")
                .fecha(Instant.now())
                .leido(false)
                .mensaje("La PQRS 'sensitive-test-title' (ID: " + local.getId() + ") fue CREADA")
        );
    }

    @AfterEach
    void clean() {
        mongo.remove(Query.query(Criteria.where("recipient").is(owner)), Notificacion.class);
        mongo.remove(theirs);
        mongo.remove(orphan);
        mongo.remove(local);
        mongo.remove(remote);
        mongo.remove(office);
        mongo.remove(elsewhere);
        mongo.remove(owner);
        mongo.remove(other);
    }

    RequestPostProcessor as(User actor, String... authorities) {
        return user(actor.getLogin()).authorities(AuthorityUtils.createAuthorityList(authorities));
    }

    @Test
    void citizenCannotReadInternalRoutes() throws Exception {
        for (String path : new String[] {
            "/api/users",
            "/api/oficinas/catalogo",
            "/api/oficinas/oficinas/" + office.getId(),
            "/api/notification",
            "/api/notification/" + mine.getId(),
            "/api/sse-notifications/subscribe",
        }) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, FRONT_DESK_CS, FUNCTIONARY, PQRSD_REPORTS })
    void catalogContainsOnlyOfficeIdAndName(String role) throws Exception {
        mvc
            .perform(get("/api/oficinas/catalogo?size=1").with(as(owner, role)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]", aMapWithSize(2)))
            .andExpect(jsonPath("$[0].nombre").exists())
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(header().exists("X-Total-Count"));
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, PQRSD_REPORTS, FUNCTIONARY, FRONT_DESK_CS })
    void privateDirectoryAndOfficeAdministrationAreRestricted(String role) throws Exception {
        var actor = as(owner, role);
        if (USER.equals(role)) mvc.perform(get("/api/oficinas/catalogo").with(actor)).andExpect(status().isForbidden());
        for (String path : new String[] {
            "/api/users",
            "/api/admin/users/oficina/" + other.getLogin(),
            "/api/oficinas",
            "/api/oficinas/search",
            "/api/oficinas/" + office.getId(),
        }) {
            mvc.perform(get(path).with(actor)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/oficinas/{id}", office.getId()).with(actor)).andExpect(status().isForbidden());
        mvc
            .perform(put("/api/oficinas/oficinas/{id}", office.getId()).with(actor).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        assertThat(mongo.findById(office.getId(), Oficina.class).getResponsable().getId()).isEqualTo(owner.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, FRONT_DESK_CS })
    void institutionalOperatorsSeeCurrentCasesAcrossOffices(String role) throws Exception {
        mvc
            .perform(get("/api/oficinas/oficinas/{id}", elsewhere.getId()).with(as(owner, role)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pqrsList[0].id").value(remote.getId()))
            .andExpect(jsonPath("$.responsableDTO").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void functionaryOnlySeesCurrentlyAssignedCases(boolean reports) throws Exception {
        var staff = reports ? as(owner, FUNCTIONARY, PQRSD_REPORTS) : as(owner, FUNCTIONARY);
        // A historical reverse link must not override the case's current assignment.
        office.addPqrs(remote);
        mongo.save(office);
        mvc
            .perform(get("/api/oficinas/oficinas/{id}", office.getId()).with(staff))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pqrsList", hasSize(1)))
            .andExpect(jsonPath("$.pqrsList[0].id").value(local.getId()))
            .andExpect(jsonPath("$.pqrsList[0].accessToken").doesNotExist());
        mvc.perform(get("/api/oficinas/oficinas/{id}", elsewhere.getId()).with(staff)).andExpect(status().isForbidden());
        mvc.perform(get("/api/oficinas/oficinas/{id}", office.getId()).with(as(other, FUNCTIONARY))).andExpect(status().isForbidden());
        mvc.perform(get("/api/sse-notifications/subscribe").with(as(other, FUNCTIONARY))).andExpect(status().isForbidden());
        mvc
            .perform(put("/api/pqrs/{id}/office", local.getId()).param("officeName", elsewhere.getNombre()).with(staff))
            .andExpect(status().isForbidden());
        assertThat(mongo.findById(local.getId(), Pqrs.class).getOficinaResponder().getId()).isEqualTo(office.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, PQRSD_REPORTS })
    void nonOperationalRolesCannotResolveACaseEvenFromOwnInbox(String role) throws Exception {
        mvc.perform(get("/api/oficinas/oficinas/{id}", office.getId()).with(as(owner, role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/notification/{id}/case", mine.getId()).with(as(owner, role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/sse-notifications/subscribe").with(as(owner, role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, PQRSD_REPORTS, FUNCTIONARY, ADMIN })
    void inboxReadsAndMutationsAreRecipientScoped(String role) throws Exception {
        var actor = as(owner, role);
        mvc
            .perform(get("/api/notification?read=false&size=1").with(actor))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "1"))
            .andExpect(jsonPath("$[0].id").value(mine.getId()));
        mvc
            .perform(get("/api/notification/{id}", mine.getId()).with(actor))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("sensitive-test-title"))));
        for (Notificacion forbidden : new Notificacion[] { theirs, orphan }) {
            mvc.perform(get("/api/notification/{id}", forbidden.getId()).with(actor)).andExpect(status().isNotFound());
            mvc.perform(put("/api/notification/{id}/mark-as-read", forbidden.getId()).with(actor)).andExpect(status().isNotFound());
            mvc.perform(delete("/api/notification/{id}", forbidden.getId()).with(actor)).andExpect(status().isNotFound());
        }
        String patch = "{\"id\":\"" + theirs.getId() + "\",\"leido\":true}";
        mvc
            .perform(patch("/api/notification/{id}", theirs.getId()).contentType(MediaType.APPLICATION_JSON).content(patch).with(actor))
            .andExpect(ADMIN.equals(role) ? status().isNotFound() : status().isForbidden());
        mvc.perform(put("/api/notification/mark-all-as-read").with(actor)).andExpect(status().isOk());
        mvc.perform(get("/api/notification?read=false").with(actor)).andExpect(header().string("X-Total-Count", "0"));
        mvc.perform(get("/api/notification").with(actor)).andExpect(header().string("X-Total-Count", "1"));
        assertThat(mongo.findById(mine.getId(), Notificacion.class).getLeido()).isTrue();
        mvc.perform(delete("/api/notification/{id}", mine.getId()).with(actor)).andExpect(status().isNoContent());
        assertThat(mongo.findById(mine.getId(), Notificacion.class)).isNull();
        assertThat(mongo.findById(theirs.getId(), Notificacion.class).getLeido()).isFalse();
        assertThat(mongo.findById(theirs.getId(), Notificacion.class).getRecipient().getId()).isEqualTo(other.getId());
        assertThat(mongo.findById(orphan.getId(), Notificacion.class).getLeido()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void eagerLoadParameterAlwaysUsesTheRecipientsPage(boolean eager) throws Exception {
        mvc
            .perform(get("/api/notification").param("eagerload", Boolean.toString(eager)).with(as(owner, USER)))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Total-Count", "1"))
            .andExpect(jsonPath("$[0].id").value(mine.getId()));
    }

    @Test
    void administratorCannotRedirectAnExistingNotice() throws Exception {
        String payload = "{\"id\":\"" + mine.getId() + "\",\"leido\":true,\"recipientDto\":{\"id\":\"" + other.getId() + "\"}}";
        mvc
            .perform(
                patch("/api/notification/{id}", mine.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
                    .with(as(owner, ADMIN))
            )
            .andExpect(status().isOk());
        Notificacion saved = mongo.findById(mine.getId(), Notificacion.class);
        assertThat(saved.getRecipient().getId()).isEqualTo(owner.getId());
        assertThat(saved.getLeido()).isTrue();
    }

    @Test
    void notificationNavigationRechecksCurrentOffice() throws Exception {
        var staff = as(owner, FUNCTIONARY);
        mvc
            .perform(get("/api/notification/{id}/case", mine.getId()).with(staff))
            .andExpect(status().isOk())
            .andExpect(content().string(local.getId()));
        local.setOficinaResponder(elsewhere);
        mongo.save(local);
        mvc.perform(get("/api/notification/{id}/case", mine.getId()).with(staff)).andExpect(status().isForbidden());
    }

    @Test
    void sseOnlyDeliversMinimalEventToAuthenticatedRecipient() throws Exception {
        var first = mvc
            .perform(
                get("/api/sse-notifications/subscribe").param("userLogin", other.getLogin()).with(as(owner, FUNCTIONARY, PQRSD_REPORTS))
            )
            .andExpect(request().asyncStarted())
            .andReturn();
        var second = mvc
            .perform(get("/api/sse-notifications/subscribe").with(as(other, FRONT_DESK_CS)))
            .andExpect(request().asyncStarted())
            .andReturn();
        try {
            delivery.sendPqrsNotification(local, PqrsNotificationType.PQRS_CREATED, owner);
            assertThat(first.getResponse().getContentAsString())
                .contains("event:PQRS_CREATED")
                .doesNotContain(owner.getLogin(), local.getId(), local.getTitulo(), "pqrsTitle", "userLogin");
            assertThat(second.getResponse().getContentAsString()).contains("event:connected").doesNotContain("PQRS_CREATED");
        } finally {
            first.getRequest().getAsyncContext().complete();
            second.getRequest().getAsyncContext().complete();
        }
    }
}
