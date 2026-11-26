package co.edu.itp.svu.service.report;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Immediate admission. The credential key and slot live only for the operation. */
@Component
class PqrsStatisticsExportAdmission {

    private final int slots;
    private final Set<String> active = new HashSet<>();

    PqrsStatisticsExportAdmission(@Value("${svu.pqrs-statistics.export.concurrent:2}") int slots) {
        if (slots < 1 || slots > 16) throw new IllegalArgumentException("Invalid export concurrency");
        this.slots = slots;
    }

    <T> T run(String credential, Supplier<T> work) {
        synchronized (active) {
            if (active.contains(credential)) throw new PqrsStatisticsReceiptException(429, "exportRateLimited");
            if (active.size() >= slots) throw PqrsStatisticsReceiptException.unavailable();
            active.add(credential);
        }
        try {
            return work.get();
        } finally {
            synchronized (active) {
                active.remove(credential);
            }
        }
    }
}
