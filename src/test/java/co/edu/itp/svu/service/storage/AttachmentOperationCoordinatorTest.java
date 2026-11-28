package co.edu.itp.svu.service.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AttachmentOperationCoordinatorTest {

    @Test
    void enforcesServerSideAttachmentCountLimit() {
        assertThatCode(() -> AttachmentOperationCoordinator.validatePartCount(20)).doesNotThrowAnyException();
        assertThatThrownBy(() -> AttachmentOperationCoordinator.validatePartCount(21))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("count limit");
    }
}
