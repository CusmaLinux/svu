package co.edu.itp.svu.security;

import static co.edu.itp.svu.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.itp.svu.domain.ArchivoAdjunto;
import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

class PqrsAuthorizationServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final OficinaRepository offices = mock(OficinaRepository.class);
    private final PqrsAuthorizationService policy = new PqrsAuthorizationService(users, offices);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(String... roles) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("operator", null, AuthorityUtils.createAuthorityList(roles))
            );
    }

    private void registerOperator() {
        User user = new User();
        user.setId("operator-id");
        when(users.findOneByLogin("operator")).thenReturn(Optional.of(user));
    }

    private void assignOffice() {
        registerOperator();
        when(offices.findByResponsable_Id("operator-id")).thenReturn(Optional.of(new Oficina().id("office-a")));
    }

    private Pqrs ownCase() {
        return new Pqrs().id("case").oficinaResponder(new Oficina().id("office-a"));
    }

    @ParameterizedTest
    @ValueSource(strings = { ADMIN, FRONT_DESK_CS })
    void institutionalOperatorsCanManageEvenUnassignedCases(String role) {
        login(role);
        assertThat(policy.requireOperationalOffice()).isEmpty();
        assertThat(policy.requireOfficeFilter("office-b")).contains("office-b");
        policy.checkCaseReadAccess(new Pqrs());
        policy.checkCaseWriteAccess(new Pqrs(), ownCase());
        policy.requireAdministrativeCaseAccess();
        verifyNoInteractions(users, offices);
    }

    @Test
    void frontDeskNeedsAdditionalReportingRoleForStatistics() {
        login(FRONT_DESK_CS);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireStatisticalAccess);
        login(FRONT_DESK_CS, PQRSD_REPORTS);
        policy.requireStatisticalAccess();
    }

    @Test
    void administratorWithMultipleRolesRetainsBothGrants() {
        login(FUNCTIONARY, PQRSD_REPORTS, ADMIN);
        assertThat(policy.requireOperationalOffice()).isEmpty();
        policy.requireAdministrativeCaseAccess();
        policy.requireStatisticalAccess();
        verifyNoInteractions(users, offices);
    }

    @Test
    void functionaryCanReadAndUpdateOnlyWithinAssignedOffice() {
        login(FUNCTIONARY);
        assignOffice();
        assertThat(policy.requireOfficeFilter(null)).contains("office-a");
        assertThat(policy.requireOfficeFilter("office-a")).contains("office-a");
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.requireOfficeFilter("office-b"));
        policy.checkCaseReadAccess(ownCase());
        policy.checkCaseWriteAccess(ownCase(), ownCase().estado("RESUELTA"));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireAdministrativeCaseAccess);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireStatisticalAccess);
    }

    @Test
    void proposedOfficeCannotOverridePersistedOffice() {
        login(FUNCTIONARY);
        assignOffice();
        Pqrs other = ownCase().oficinaResponder(new Oficina().id("office-b"));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseReadAccess(other));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseWriteAccess(other, ownCase()));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseWriteAccess(ownCase(), other));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseReadAccess(new Pqrs()));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseWriteAccess(new Pqrs(), ownCase()));
    }

    static Stream<Consumer<Pqrs>> protectedFieldChanges() {
        return Stream.of(
            p -> p.setDaysToReply(2),
            p -> p.setFechaLimiteRespuesta(Instant.EPOCH),
            p -> p.setFechaCreacion(Instant.EPOCH),
            p -> p.setFileNumber("synthetic-case"),
            p -> p.setAccessToken("synthetic-token"),
            p -> p.setRequesterEmail("example@example.invalid"),
            p -> p.setType(PqrsType.REQUEST),
            p -> p.setTitulo("changed"),
            p -> p.addArchivosAdjuntos(new ArchivoAdjunto().id("document"))
        );
    }

    @ParameterizedTest
    @MethodSource("protectedFieldChanges")
    void functionaryCannotChangeProtectedFields(Consumer<Pqrs> change) {
        login(FUNCTIONARY);
        assignOffice();
        Pqrs proposed = ownCase();
        change.accept(proposed);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseWriteAccess(ownCase(), proposed));
    }

    @Test
    void unresolvedOrAmbiguousAssignmentDeniesAccess() {
        login(FUNCTIONARY);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
        registerOperator();
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
        when(offices.findByResponsable_Id("operator-id")).thenReturn(Optional.of(new Oficina().id(" ")));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
        when(offices.findByResponsable_Id("operator-id")).thenThrow(new IncorrectResultSizeDataAccessException(1, 2));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
    }

    @ParameterizedTest
    @ValueSource(strings = { USER, PQRSD_REPORTS })
    void nonOperationalRolesCannotAccessCases(String role) {
        login(role);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseReadAccess(ownCase()));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.checkCaseWriteAccess(ownCase(), ownCase()));
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireAdministrativeCaseAccess);
        verifyNoInteractions(users, offices);
    }

    @Test
    void reportingGrantIsIndependentOfOfficeAssignment() {
        login(USER);
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireStatisticalAccess);
        login(PQRSD_REPORTS);
        policy.requireStatisticalAccess();
        login(PQRSD_REPORTS, FUNCTIONARY);
        policy.requireStatisticalAccess();
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(policy::requireOperationalOffice);
        assignOffice();
        assertThat(policy.requireOperationalOffice()).contains("office-a");
        assertThatExceptionOfType(AccessDeniedException.class).isThrownBy(() -> policy.requireOfficeFilter("office-b"));
    }
}
