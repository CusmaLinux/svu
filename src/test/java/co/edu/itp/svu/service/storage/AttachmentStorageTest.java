package co.edu.itp.svu.service.storage;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentStorageTest {

    @TempDir
    Path root;

    @Test
    void writesAndReadsExactBytesUsingOpaqueKey() throws IOException {
        var storage = new AttachmentStorage(root, UUID::randomUUID);
        byte[] bytes = { 0, 1, -1, 42 };
        String first = storage.write(new ByteArrayInputStream(bytes));
        String second = storage.write(new ByteArrayInputStream(bytes));
        assertThat(first).isNotEqualTo(second).isEqualTo(UUID.fromString(first).toString());
        try (var input = storage.open(first)) {
            assertThat(input.readAllBytes()).containsExactly(bytes);
        }
        storage.delete(first);
        assertFailure(() -> storage.open(first), AttachmentStorage.Failure.MISSING);
        assertThat(root.resolve(second)).exists();
    }

    @Test
    void revalidatesPersistedGenerationWithoutDeletingOnMismatch() throws Exception {
        var storage = new AttachmentStorage(root, UUID::randomUUID);
        byte[] bytes = "verified synthetic bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String key = storage.write(new ByteArrayInputStream(bytes));
        String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));

        assertThat(storage.verifyPersisted(UUID.randomUUID().toString(), key, bytes.length, hash)).isPresent();
        assertThat(storage.verifyPersisted(UUID.randomUUID().toString(), key, bytes.length, "0".repeat(64))).isEmpty();
        assertThat(root.resolve(key)).exists().hasBinaryContent(bytes);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Informe con tildes á.pdf", "sin extensión" })
    void readsHistoricalKeysExactly(String name) throws IOException {
        Files.writeString(root.resolve(name), "contenido sintético");
        try (var input = new AttachmentStorage(root, UUID::randomUUID).open(name)) {
            assertThat(input.readAllBytes()).isEqualTo(Files.readAllBytes(root.resolve(name)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "", ".", "..", "../synthetic", "/synthetic", "sub/file", "sub\\file", "C:synthetic", "a\nb", "\uD800" })
    void rejectsInvalidKeys(String key) {
        var storage = new AttachmentStorage(root, UUID::randomUUID);
        assertFailure(() -> storage.open(key), AttachmentStorage.Failure.INVALID_KEY);
        assertFailure(() -> storage.delete(key), AttachmentStorage.Failure.INVALID_KEY);
    }

    @Test
    void rejectsLinksDirectoriesAndMissingEntries() throws IOException {
        var storage = new AttachmentStorage(root, UUID::randomUUID);
        var directory = Files.createDirectory(root.resolve("directory"));
        var sentinel = Files.writeString(directory.resolve("synthetic"), "intacto");
        Files.createSymbolicLink(root.resolve("link"), sentinel);
        for (String name : new String[] { "link", "directory" }) {
            assertFailure(() -> storage.open(name), AttachmentStorage.Failure.UNSAFE_ENTRY);
            assertFailure(() -> storage.delete(name), AttachmentStorage.Failure.UNSAFE_ENTRY);
        }
        assertFailure(() -> storage.delete("missing"), AttachmentStorage.Failure.MISSING);
        assertThat(sentinel).hasContent("intacto");
    }

    @Test
    void refusesLinkedRootAndLinkedAncestor() throws IOException {
        var directory = Files.createDirectories(root.resolve("directory/sub"));
        Files.createSymbolicLink(root.resolve("link"), directory.getParent());
        for (Path path : new Path[] { root.resolve("link"), root.resolve("link/sub"), root.resolve("absent") }) {
            var storage = new AttachmentStorage(path, UUID::randomUUID);
            assertFailure(() -> storage.write(InputStream.nullInputStream()), AttachmentStorage.Failure.UNAVAILABLE);
        }
    }

    @Test
    void collisionDoesNotOverwriteAnExistingFile() throws IOException {
        var key = UUID.randomUUID();
        var file = Files.writeString(root.resolve(key.toString()), "original");
        var storage = new AttachmentStorage(root, () -> key);
        assertFailure(() -> storage.write(new ByteArrayInputStream(new byte[] { 1 })), AttachmentStorage.Failure.WRITE_FAILED);
        assertThat(file).hasContent("original");
    }

    @Test
    void removesIncompleteWriteAndRedactsSourceException() {
        var key = UUID.randomUUID();
        var storage = new AttachmentStorage(root, () -> key);
        var failing = new InputStream() {
            private boolean started;

            @Override
            public int read() throws IOException {
                if (started) {
                    throw new IOException("synthetic private detail");
                }
                started = true;
                return 42;
            }
        };
        assertFailure(() -> storage.write(failing), AttachmentStorage.Failure.WRITE_FAILED);
        assertThat(root.resolve(key.toString())).doesNotExist();
    }

    private void assertFailure(ThrowableAssert.ThrowingCallable operation, AttachmentStorage.Failure failure) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(AttachmentStorage.StorageException.class, e -> {
            assertThat(e.failure()).isEqualTo(failure);
            assertThat(e).hasMessage(failure.name()).hasNoCause();
        });
    }
}
