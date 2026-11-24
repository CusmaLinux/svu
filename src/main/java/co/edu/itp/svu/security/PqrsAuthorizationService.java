package co.edu.itp.svu.security;

import co.edu.itp.svu.domain.Oficina;
import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.repository.OficinaRepository;
import co.edu.itp.svu.repository.UserRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PqrsAuthorizationService {

    private final UserRepository userRepository;
    private final OficinaRepository oficinaRepository;

    public PqrsAuthorizationService(UserRepository userRepository, OficinaRepository oficinaRepository) {
        this.userRepository = userRepository;
        this.oficinaRepository = oficinaRepository;
    }

    private boolean hasInstitutionalCaseAccess() {
        return SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.FRONT_DESK_CS);
    }

    public void requireAdministrativeCaseAccess() {
        if (!hasInstitutionalCaseAccess()) {
            throw new AccessDeniedException("Only administration or front desk can manage cases");
        }
    }

    public void requireStatisticalAccess() {
        if (!SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN, AuthoritiesConstants.PQRSD_REPORTS)) {
            throw new AccessDeniedException("PQRSD statistics access denied");
        }
    }

    /** Empty means authorized institutional access. An unresolved office or unauthorized role always throws. */
    public Optional<String> requireOperationalOffice() {
        if (hasInstitutionalCaseAccess()) {
            return Optional.empty();
        }
        if (!SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.FUNCTIONARY)) {
            throw new AccessDeniedException("PQRSD access denied");
        }
        return Optional.of(requireFunctionaryOffice());
    }

    public Optional<String> requireResponseCreationOffice() {
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            return Optional.empty();
        }
        if (!SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.FUNCTIONARY)) {
            throw new AccessDeniedException("PQRSD response creation denied");
        }
        return Optional.of(requireFunctionaryOffice());
    }

    public void requireResponseManagementAccess() {
        if (!SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            throw new AccessDeniedException("PQRSD response management denied");
        }
    }

    private String requireFunctionaryOffice() {
        String userId = SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .map(User::getId)
            .filter(id -> !id.isBlank())
            .orElseThrow(() -> new AccessDeniedException("Missing PQRSD operator"));
        try {
            return oficinaRepository
                .findByResponsable_Id(userId)
                .map(Oficina::getId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new AccessDeniedException("No office assigned"));
        } catch (IncorrectResultSizeDataAccessException ex) {
            throw new AccessDeniedException("Ambiguous office assignment", ex);
        }
    }

    public Optional<String> requireOfficeFilter(String requestedOfficeId) {
        Optional<String> requested = Optional.ofNullable(requestedOfficeId).filter(id -> !id.isBlank());
        Optional<String> authorized = requireOperationalOffice();
        if (authorized.isPresent() && requested.isPresent() && !authorized.equals(requested)) {
            throw new AccessDeniedException("Cannot list another office");
        }
        return authorized.or(() -> requested);
    }

    /** Call with the case loaded from the repository. */
    public void checkCaseReadAccess(Pqrs persisted) {
        checkCaseInAuthorizedOffice(persisted, requireOperationalOffice());
    }

    /** Use only a scope resolved by the authorization policy for the requested action. */
    public void checkCaseInAuthorizedOffice(Pqrs persisted, Optional<String> authorizedOffice) {
        authorizedOffice.ifPresent(officeId -> requireCaseInOffice(persisted, officeId));
    }

    private void requireCaseInOffice(Pqrs pqrs, String officeId) {
        if (pqrs.getOficinaResponder() == null || !officeId.equals(pqrs.getOficinaResponder().getId())) {
            throw new AccessDeniedException("PQRSD belongs to another office");
        }
    }

    /** Check both the persisted case and the proposed replacement before any side effects. */
    public void checkCaseWriteAccess(Pqrs existing, Pqrs proposed) {
        Optional<String> office = requireOperationalOffice();
        if (office.isEmpty()) {
            return;
        }
        String officeId = office.orElseThrow();
        requireCaseInOffice(existing, officeId);
        requireCaseInOffice(proposed, officeId);
        if (
            !Objects.equals(existing.getDaysToReply(), proposed.getDaysToReply()) ||
            !Objects.equals(existing.getFechaLimiteRespuesta(), proposed.getFechaLimiteRespuesta()) ||
            !Objects.equals(existing.getFechaCreacion(), proposed.getFechaCreacion()) ||
            !Objects.equals(existing.getFileNumber(), proposed.getFileNumber()) ||
            !Objects.equals(existing.getAccessToken(), proposed.getAccessToken()) ||
            !Objects.equals(existing.getRequesterEmail(), proposed.getRequesterEmail()) ||
            !Objects.equals(existing.getType(), proposed.getType()) ||
            !Objects.equals(existing.getTitulo(), proposed.getTitulo()) ||
            !Objects.equals(existing.getArchivosAdjuntos(), proposed.getArchivosAdjuntos())
        ) {
            throw new AccessDeniedException("Functionary may not alter case identity, requester or deadlines");
        }
    }
}
