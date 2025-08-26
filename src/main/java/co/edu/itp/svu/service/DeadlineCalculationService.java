package co.edu.itp.svu.service;

import co.edu.itp.svu.domain.NonBusinessDay;
import co.edu.itp.svu.repository.NonBusinessDayRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DeadlineCalculationService {

    private final NonBusinessDayRepository nonBusinessDayRepository;
    private final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    public DeadlineCalculationService(NonBusinessDayRepository nonBusinessDayRepository) {
        this.nonBusinessDayRepository = nonBusinessDayRepository;
    }

    /**
     * Calculates the deadline by adding a number of business days to a start date.
     * This method skips weekends (Saturday, Sunday) and any special dates found in the database.
     *
     * @param startDate The starting date of the calculation.
     * @param businessDaysToAdd The number of business days to add.
     * @return The calculated deadline as an Instant.
     */
    public Instant calculateDeadline(Instant startDate, int businessDaysToAdd) {
        LocalDate initialDate = startDate.atZone(BOGOTA_ZONE).toLocalDate();
        Set<LocalDate> specialDates = nonBusinessDayRepository
            .findAllByDateAfter(initialDate.minusDays(1))
            .stream()
            .map(NonBusinessDay::getDate)
            .collect(Collectors.toSet());

        LocalDate deadlineDate = initialDate;
        int daysAdded = 0;

        while (daysAdded < businessDaysToAdd) {
            deadlineDate = deadlineDate.plusDays(1);
            DayOfWeek dayOfWeek = deadlineDate.getDayOfWeek();

            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY && !specialDates.contains(deadlineDate)) {
                daysAdded++;
            }
        }

        return deadlineDate.atStartOfDay(BOGOTA_ZONE).toInstant();
    }
}
