package co.edu.itp.svu.service.report;

import co.edu.itp.svu.service.report.PqrsStatisticsDocumentService.ExportRequest;
import java.io.IOException;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class PqrsStatisticsWordExport {

    private final PqrsStatisticsNarrativeService previews;
    private final PqrsStatisticsDocumentService documents;
    private final PqrsStatisticsDocx renderer;
    private final PqrsStatisticsExportAdmission admission;

    PqrsStatisticsWordExport(
        PqrsStatisticsNarrativeService previews,
        PqrsStatisticsDocumentService documents,
        PqrsStatisticsDocx renderer,
        PqrsStatisticsExportAdmission admission
    ) {
        this.previews = previews;
        this.documents = documents;
        this.renderer = renderer;
        this.admission = admission;
    }

    public record File(byte[] bytes, String name) {
        @Override
        public String toString() {
            return "Generated PQRSD document";
        }
    }

    public File generate(Supplier<ExportRequest> body) {
        return admission.run(previews.exportCredentialKey(), () -> {
            var request = body.get();
            var document = documents.prepare(request);
            byte[] bytes;
            try {
                bytes = renderer.generate(document);
            } catch (PqrsStatisticsReceiptException controlled) {
                throw controlled;
            } catch (IOException | RuntimeException failure) {
                throw PqrsStatisticsReceiptException.unavailable();
            }
            previews.recheckValidity(request.filter(), request.receipt());
            var filter = document.metadata().filter();
            String scope = filter.officeId() == null ? "Institucional" : "Dependencia";
            return new File(bytes, "Informe_PQRSD_Preliminar_" + scope + "_" + filter.start() + "_" + filter.end() + ".docx");
        });
    }
}
