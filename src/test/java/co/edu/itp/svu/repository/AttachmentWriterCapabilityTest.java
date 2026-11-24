package co.edu.itp.svu.repository;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttachmentWriterCapabilityTest {

    @Test
    void bindsFreshProofAndStableKeysToTheOriginalReservation() {
        UUID operation = UUID.randomUUID();
        var writer = new AttachmentWriterCapability(operation, 1);
        var proof = writer.proof(operation.toString(), 1);
        assertThat(proof).matches("[0-9a-f]{64}");
        assertThat(new AttachmentWriterCapability(operation, 1).proof(operation.toString(), 1)).isNotEqualTo(proof);
        assertThat(writer.prepareKeys(2)).doesNotHaveDuplicates().isEqualTo(writer.prepareKeys(2));
        assertThatThrownBy(() -> writer.proof(operation.toString(), 2)).hasMessage("Writer capability belongs to another reservation");
        assertThatThrownBy(() -> writer.proof(UUID.randomUUID().toString(), 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposesNeitherProofNorSecretThroughRepresentationOrJackson() {
        UUID operation = UUID.randomUUID();
        var writer = new AttachmentWriterCapability(operation, 1);
        assertThat(writer.toString()).isEqualTo("AttachmentWriterCapability[redacted]");
        assertThat(
            new ObjectMapper()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .valueToTree(writer)
                .toString()
        ).isEqualTo("{}");
    }
}
