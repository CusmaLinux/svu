package co.edu.itp.svu.repository;

import static co.edu.itp.svu.domain.attachment.AttachmentOperationState.*;
import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import co.edu.itp.svu.service.storage.AttachmentRequestIdentity;
import java.security.SecureRandom;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AttachmentManifestTest {

    @ParameterizedTest
    @EnumSource(AttachmentOperationState.class)
    void checksEveryDestination(AttachmentOperationState from) {
        var allowed = Map.of(
            PREPARING,
            Set.of(FILES_READY, ABORTED, RECONCILIATION_REQUIRED),
            FILES_READY,
            Set.of(PERSISTING, ABORTED, RECONCILIATION_REQUIRED),
            PERSISTING,
            Set.of(RECONCILIATION_REQUIRED)
        );
        for (var target : AttachmentOperationState.values()) {
            assertThat(from.permits(target)).as("%s -> %s", from, target).isEqualTo(allowed.getOrDefault(from, Set.of()).contains(target));
        }
    }

    @Test
    void canonicalizesNamedValuesAndFramesScopesFilesAndKeys() {
        var key = new byte[32];
        new SecureRandom().nextBytes(key);
        var mac = new AttachmentRequestIdentity(key);
        var type = AttachmentOperationType.PUBLIC_CASE_CREATE;
        var file = new AttachmentRequestIdentity.FileInput("sintético", "text/plain", 0, "a".repeat(64));
        var other = new AttachmentRequestIdentity.FileInput("segundo", "text/plain", 1, "b".repeat(64));
        var fields = new TreeMap<String, String>(Comparator.reverseOrder());
        fields.put("z", "");
        fields.put("a", "dato sintético");
        var scope = List.of("ab", "c");
        var files = List.of(file, other);
        var id = mac.derive(scope, type, null, "clave", fields, files);
        assertThat(id).isEqualTo(mac.derive(scope, type, null, "clave", new TreeMap<>(fields), files));
        assertThat(id.toString()).doesNotContain("clave", "dato sintético", "text/plain");
        assertThat(mac.derive(List.of("a", "bc"), type, null, "clave", fields, files).actorScopeHash()).isNotEqualTo(id.actorScopeHash());
        assertThat(
            mac.derive(scope, AttachmentOperationType.CITIZEN_RESPONSE_CREATE, "synthetic-target", "clave", fields, files).requestKeyHash()
        ).isNotEqualTo(id.requestKeyHash());
        assertThat(mac.derive(scope, type, null, "nueva", fields, files).requestKeyHash()).isNotEqualTo(id.requestKeyHash());
        assertThat(mac.derive(scope, type, null, "clave", Map.of("a", "dato sintético"), files).requestDigest()).isNotEqualTo(
            id.requestDigest()
        );
        assertThat(mac.derive(scope, type, null, "clave", fields, List.of(other, file)).requestDigest()).isNotEqualTo(id.requestDigest());
        assertThatThrownBy(() -> new AttachmentRequestIdentity(new byte[16])).isInstanceOf(IllegalArgumentException.class);
        new SecureRandom().nextBytes(key);
        assertThat(new AttachmentRequestIdentity(key).derive(scope, type, null, "clave", fields, files)).isNotEqualTo(id);
    }
}
