package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.Pqrs;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.repository.PqrsRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PqrsDeadlineAdjustmentService {

    private final Logger log = LoggerFactory.getLogger(PqrsDeadlineAdjustmentService.class);
    private final PqrsRepository pqrsRepository;
    private final DeadlineCalculationService deadlineCalculationService;
    private final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    public PqrsDeadlineAdjustmentService(PqrsRepository pqrsRepository, DeadlineCalculationService deadlineCalculationService) {
        this.pqrsRepository = pqrsRepository;
        this.deadlineCalculationService = deadlineCalculationService;
    }

    /**
     * Finds all active PQRS affected by a change on a specific date and recalculates their deadlines.
     * @param changedDate The date that was added or removed as a non-business day.
     */
    public void adjustDeadlinesForDateChange(LocalDate changedDate) {
        log.info("Adjusting PQRS deadlines due to a calendar change on: {}", changedDate);

        List<String> inactiveStatuses = List.of(PqrsStatus.RESOLVED.toString(), PqrsStatus.CLOSED.toString());

        // Find all active PQRS whose original deadline calculation window includes the changed date.
        // This means their creation date is before the changed date, and their deadline is after it.
        Instant changedDateInstant = changedDate.atStartOfDay(BOGOTA_ZONE).toInstant();
        List<Pqrs> affectedPqrs = pqrsRepository.findAllByEstadoNotInAndFechaCreacionBeforeAndFechaLimiteRespuestaAfter(
            inactiveStatuses,
            changedDateInstant,
            changedDateInstant
        );

        if (affectedPqrs.isEmpty()) {
            log.info("No active PQRS were affected by the change on {}", changedDate);
            return;
        }

        log.info("Found {} PQRS to recalculate.", affectedPqrs.size());

        List<Pqrs> updatedPqrsList = affectedPqrs
            .stream()
            .map(pqrs -> {
                Instant newDeadline = deadlineCalculationService.calculateDeadline(pqrs.getFechaCreacion(), pqrs.getDaysToReply());
                pqrs.setFechaLimiteRespuesta(newDeadline);
                return pqrs;
            })
            .collect(Collectors.toList());

        pqrsRepository.saveAll(updatedPqrsList);
        log.info("Successfully updated deadlines for {} PQRS.", updatedPqrsList.size());
    }
}
