package co.edu.itp.svu.scheduler;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.User;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.repository.PqrsRepository;
import co.edu.itp.svu.service.notification.PqrsNotificationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PqrsScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(PqrsScheduler.class);

    private final PqrsRepository pqrsRepository;
    private final PqrsNotificationService pqrsNotificationService;

    public PqrsScheduler(PqrsRepository pqrsRepository, PqrsNotificationService pqrsNotificationService) {
        this.pqrsRepository = pqrsRepository;
        this.pqrsNotificationService = pqrsNotificationService;
    }

    @Scheduled(cron = "0 0 9 * * ?", zone = "America/Bogota")
    public void checkPqrsDueDates() {
        LOG.info("Running PQRS due date check...");

        Instant now = Instant.now();
        Instant threeDaysFromNow = now.plus(3, ChronoUnit.DAYS);

        List<Pqrs> upcomingPqrs = pqrsRepository.findAllByFechaLimiteRespuestaBetweenAndEstadoNotIn(
            now,
            threeDaysFromNow,
            List.of(PqrsStatus.RESOLVED.toString(), PqrsStatus.CLOSED.toString())
        );

        for (Pqrs pqrs : upcomingPqrs) {
            LOG.debug(
                "Found upcoming PQRS: id={}, title={}, dueDate={}",
                pqrs.getTitulo(),
                pqrs.getTitulo(),
                pqrs.getFechaLimiteRespuesta()
            );

            if (pqrs.getOficinaResponder() != null) {
                User responsibleUser = pqrs.getOficinaResponder().getResponsable();

                if (responsibleUser != null) {
                    pqrsNotificationService.sendPqrsNotification(
                        pqrs,
                        PqrsNotificationService.PqrsNotificationType.PQRS_DUE_DATE_REMINDER,
                        responsibleUser
                    );
                }
            } else {
                LOG.warn("PQRS with id {} has no OficinaResponder, cannot determine responsible user.", pqrs.getId());
            }
        }
        LOG.info("PQRS due date check finished.");
    }
}
