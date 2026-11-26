package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.itp.svu.service.report.PqrsStatisticsDocumentService.ExportRequest;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class PqrsStatisticsWordExportTest {

    @Test
    void enforcesPerCredentialAndJvmCapsAndReleasesSlotsOnFailure() throws Exception {
        var admission = new PqrsStatisticsExportAdmission(2);
        var start = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (String key : java.util.List.of("synthetic-a", "synthetic-b")) executor.submit(() ->
                admission.run(key, () -> {
                    start.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timeout");
                    } catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(failure);
                    }
                    return key;
                })
            );
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> admission.run("synthetic-a", () -> "unexpected")).hasMessage("exportRateLimited");
            assertThatThrownBy(() -> admission.run("synthetic-c", () -> "unexpected")).hasMessage("exportUnavailable");
        } finally {
            release.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThatThrownBy(() ->
            admission.run("synthetic-a", () -> {
                throw new IllegalStateException("test");
            })
        ).hasMessage("test");
        assertThat(admission.run("synthetic-a", () -> "complete")).isEqualTo("complete");
    }

    @Test
    void checksExpiryAfterRenderingAndReleasesAdmissionOnFailure() throws Exception {
        var previews = mock(PqrsStatisticsNarrativeService.class);
        var documents = mock(PqrsStatisticsDocumentService.class);
        var renderer = mock(PqrsStatisticsDocx.class);
        var admission = new PqrsStatisticsExportAdmission(1);
        var request = new ExportRequest(
            "synthetic",
            PqrsStatisticsReceiptTest.FILTER,
            PqrsStatisticsNarrativeText.validate(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode())
        );
        var source = new PqrsStatisticsDocument(null, java.util.List.of());
        when(previews.exportCredentialKey()).thenReturn("synthetic");
        when(documents.prepare(request)).thenReturn(source);
        when(renderer.generate(source)).thenReturn(new byte[] { 1, 2, 3 });
        doThrow(new PqrsStatisticsReceiptException(410, "previewExpired"))
            .when(previews)
            .recheckValidity(request.filter(), request.receipt());
        var exporter = new PqrsStatisticsWordExport(previews, documents, renderer, admission);
        assertThatThrownBy(() -> exporter.generate(() -> request)).hasMessage("previewExpired");
        var ordered = inOrder(previews, documents, renderer);
        ordered.verify(previews).exportCredentialKey();
        ordered.verify(documents).prepare(request);
        ordered.verify(renderer).generate(source);
        ordered.verify(previews).recheckValidity(request.filter(), request.receipt());
        ordered.verifyNoMoreInteractions();
        assertThat(admission.run("synthetic", () -> "released")).isEqualTo("released");
    }
}
