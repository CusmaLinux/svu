package co.edu.itp.svu.service.storage;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import co.edu.itp.svu.repository.AttachmentOperationRepository.PhysicalWriteContext;
import co.edu.itp.svu.repository.AttachmentOperationRepository.ReservedFile;
import co.edu.itp.svu.service.storage.AttachmentStorage.Failure;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AttachmentPhysicalPreparationTest {

    @TempDir
    Path root;

    private static final byte[] DATA = { 1, 2, 3 };
    private static final byte[] EMPTY = {};

    private ReservedFile reserved(byte[] bytes) throws Exception {
        return new ReservedFile(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            bytes.length,
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
        );
    }

    private PhysicalWriteContext context(ReservedFile... files) {
        // The persistence boundary and real context issuance are exercised in the standalone suite.
        var context = mock(PhysicalWriteContext.class);
        when(context.start()).thenReturn(true, false);
        when(context.current()).thenReturn(true);
        when(context.files()).thenReturn(List.of(files));
        return context;
    }

    private AttachmentStorage storage() {
        return new AttachmentStorage(root, () -> {
            throw new AssertionError("Physical keys must already be reserved");
        });
    }

    private static InputStream source(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static void fails(Failure failure, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(AttachmentStorage.StorageException.class, ex -> {
            assertThat(ex.failure()).isEqualTo(failure);
            assertThat(ex).hasMessage(failure.name()).hasNoCause();
        });
    }

    @Test
    void preparesSingleFileThroughRealSecureChannels() throws Exception {
        var file = reserved(DATA);
        var result = storage().prepare(context(file), Map.of(file.logicalId(), source(DATA)));
        assertThat(result.files()).containsExactly(
            new AttachmentStorage.PreparedFile(file.logicalId(), file.storageKey(), DATA.length, file.expectedSha256())
        );
        assertThat(result.toString()).isEqualTo("PreparedGeneration[redacted]");
        Path path = root.resolve(file.storageKey());
        assertThat(Files.readAllBytes(path)).containsExactly(DATA);
        assertThat(Files.getPosixFilePermissions(path)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
    }

    @Test
    void preparesMultipleFilesIncludingEmptyBytesAndClosesAllDescriptors() throws Exception {
        var empty = reserved(EMPTY);
        var file = reserved(DATA);
        List<SeekableByteChannel> opened = new ArrayList<>();
        List<SeekableByteChannel> synced = new ArrayList<>();
        var io = new AttachmentStorage.DurableIo() {
            @Override
            SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean isDirectory) throws IOException {
                var channel = super.open(directory, key, isDirectory);
                opened.add(channel);
                return channel;
            }

            @Override
            void force(FileChannel channel) throws IOException {
                super.force(channel);
                synced.add(channel);
                if (channel == opened.get(0)) assertThat(opened.subList(1, opened.size())).noneMatch(SeekableByteChannel::isOpen);
            }
        };
        var storage = new AttachmentStorage(root, UUID::randomUUID, DATA.length, DATA.length, io);
        var context = context(empty, file);
        var result = storage.prepare(context, Map.of(empty.logicalId(), source(EMPTY), file.logicalId(), source(DATA)));
        assertThat(result.files()).extracting(AttachmentStorage.PreparedFile::size).containsExactly(0L, 3L);
        assertThat(synced).containsExactly(opened.get(1), opened.get(2), opened.get(0));
        assertThat(opened).noneMatch(SeekableByteChannel::isOpen);
        assertThat(Files.size(root.resolve(empty.storageKey()))).isZero();
        fails(Failure.CONTEXT_CONSUMED, () -> storage.prepare(context, Map.of()));
    }

    @Test
    void preservesSmallerPartialAndStopsBeforeWritingExcessBytes() throws Exception {
        var small = reserved(DATA);
        fails(Failure.SIZE_MISMATCH, () -> storage().prepare(context(small), Map.of(small.logicalId(), source(new byte[] { 1 }))));
        assertThat(Files.readAllBytes(root.resolve(small.storageKey()))).containsExactly(1);
        var large = reserved(EMPTY);
        fails(Failure.SIZE_MISMATCH, () -> storage().prepare(context(large), Map.of(large.logicalId(), source(DATA))));
        assertThat(Files.size(root.resolve(large.storageKey()))).isZero();
    }

    @Test
    void rejectsDigestMismatchAndRetainsTheBytes() throws Exception {
        var file = reserved(new byte[] { 3, 2, 1 });
        fails(Failure.DIGEST_MISMATCH, () -> storage().prepare(context(file), Map.of(file.logicalId(), source(DATA))));
        assertThat(Files.readAllBytes(root.resolve(file.storageKey()))).containsExactly(DATA);
    }

    @Test
    void refusesExistingBytesLinksAndDirectoryEntries() throws Exception {
        var file = reserved(DATA);
        var link = reserved(DATA);
        var directory = reserved(DATA);
        Path existing = Files.write(root.resolve(file.storageKey()), DATA);
        Files.createSymbolicLink(root.resolve(link.storageKey()), existing);
        Files.createDirectory(root.resolve(directory.storageKey()));
        for (var reserved : List.of(file, link, directory)) {
            fails(Failure.COLLISION, () -> storage().prepare(context(reserved), Map.of(reserved.logicalId(), source(DATA))));
        }
        assertThat(Files.readAllBytes(existing)).containsExactly(DATA);
        assertThat(Files.isSymbolicLink(root.resolve(link.storageKey()))).isTrue();
        assertThat(Files.isDirectory(root.resolve(directory.storageKey()))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "/absolute", "../escape", "nested/name", "1-1-1-1-1", "C:\\escaped" })
    void rejectsNonCanonicalKeysBeforeCreatingAnyFile(String key) throws Exception {
        var logical = reserved(DATA);
        var file = new ReservedFile(logical.logicalId(), key, logical.expectedSize(), logical.expectedSha256());
        fails(Failure.INVALID_KEY, () -> storage().prepare(context(file), Map.of(file.logicalId(), source(DATA))));
        try (var entries = Files.list(root)) {
            assertThat(entries).isEmpty();
        }
    }

    @Test
    void rejectsMissingSourceAndTechnicalBudgetBeforeOpening() throws Exception {
        var file = reserved(DATA);
        var second = reserved(DATA);
        var limited = new AttachmentStorage(root, UUID::randomUUID, 3, 5, new AttachmentStorage.DurableIo());
        fails(Failure.INVALID_BATCH, () -> limited.prepare(context(file), Map.of()));
        fails(Failure.SIZE_MISMATCH, () ->
            limited.prepare(context(file, second), Map.of(file.logicalId(), source(DATA), second.logicalId(), source(DATA)))
        );
        assertThat(root.resolve(file.storageKey())).doesNotExist();
    }

    @Test
    void retainsCompletedAndPartialFilesAfterSourceFailure() throws Exception {
        var first = reserved(DATA);
        var second = reserved(DATA);
        var badInput = new InputStream() {
            private boolean read;

            @Override
            public int read(byte[] buffer) throws IOException {
                if (read) throw new IOException("/secret/path token synthetic");
                buffer[0] = DATA[0];
                read = true;
                return 1;
            }

            @Override
            public int read() {
                throw new AssertionError("Expected streaming read");
            }
        };
        fails(Failure.READ_FAILED, () ->
            storage().prepare(context(first, second), Map.of(first.logicalId(), source(DATA), second.logicalId(), badInput))
        );
        assertThat(Files.readAllBytes(root.resolve(first.storageKey()))).containsExactly(DATA);
        assertThat(Files.readAllBytes(root.resolve(second.storageKey()))).containsExactly(1);
    }

    @Test
    void sanitizesWriteFailureAndClosesTheChannel() throws Exception {
        FileChannel channel = mock(FileChannel.class);
        when(channel.write(any(ByteBuffer.class))).thenThrow(new IOException("/secret/path"));
        var io = new AttachmentStorage.DurableIo() {
            @Override
            SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean isDirectory) throws IOException {
                return isDirectory ? super.open(directory, key, true) : channel;
            }
        };
        var storage = new AttachmentStorage(root, UUID::randomUUID, 10, 10, io);
        var file = reserved(DATA);
        fails(Failure.WRITE_FAILED, () -> storage.prepare(context(file), Map.of(file.logicalId(), source(DATA))));
        verify(channel).close();
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void failsClosedAndKeepsTheObjectWhenFileSyncDirectorySyncOrCloseFails(int failureStage) throws Exception {
        var io = new AttachmentStorage.DurableIo() {
            private SeekableByteChannel parent;

            @Override
            SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean isDirectory) throws IOException {
                var channel = super.open(directory, key, isDirectory);
                if (isDirectory) parent = channel;
                return channel;
            }

            @Override
            void force(FileChannel channel) throws IOException {
                if (failureStage == (channel == parent ? 2 : 1)) throw new IOException("/secret/path");
                super.force(channel);
            }

            @Override
            void close(SeekableByteChannel channel) throws IOException {
                super.close(channel);
                if (failureStage == 3 && channel != parent) throw new IOException("/secret/path");
            }
        };
        var storage = new AttachmentStorage(root, UUID::randomUUID, 10, 10, io);
        var file = reserved(DATA);
        fails(failureStage == 3 ? Failure.CLOSE_FAILED : Failure.SYNC_FAILED, () ->
            storage.prepare(context(file), Map.of(file.logicalId(), source(DATA)))
        );
        assertThat(Files.readAllBytes(root.resolve(file.storageKey()))).containsExactly(DATA);
    }

    @Test
    void refusesProviderWithoutFileChannelAndClosesTheReturnedDescriptor() throws Exception {
        SeekableByteChannel channel = mock(SeekableByteChannel.class);
        var io = new AttachmentStorage.DurableIo() {
            @Override
            SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean isDirectory) {
                return channel;
            }
        };
        var storage = new AttachmentStorage(root, UUID::randomUUID, 10, 10, io);
        var file = reserved(DATA);
        fails(Failure.UNAVAILABLE, () -> storage.prepare(context(file), Map.of(file.logicalId(), source(DATA))));
        assertThat(root.resolve(file.storageKey())).doesNotExist();
        verify(channel).close();
    }

    @Test
    void checksLeaseAgainAfterDirectorySyncAndClose() throws Exception {
        var file = reserved(DATA);
        var context = context(file);
        when(context.current()).thenReturn(true, true, false);
        fails(Failure.CLAIM_LOST, () -> storage().prepare(context, Map.of(file.logicalId(), source(DATA))));
        assertThat(Files.readAllBytes(root.resolve(file.storageKey()))).containsExactly(DATA);
    }

    @Test
    void rejectsAProviderThatLeavesTheWriterOpenOnClose() throws Exception {
        var channels = new ArrayList<SeekableByteChannel>();
        var io = new AttachmentStorage.DurableIo() {
            @Override
            SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean parent) throws IOException {
                var channel = super.open(directory, key, parent);
                channels.add(channel);
                return channel;
            }

            @Override
            void close(SeekableByteChannel channel) throws IOException {
                if (channel == channels.get(0)) super.close(channel);
            }
        };
        try {
            var storage = new AttachmentStorage(root, UUID::randomUUID, 10, 10, io);
            var file = reserved(DATA);
            var context = context(file);
            fails(Failure.CLOSE_FAILED, () -> storage.prepare(context, Map.of(file.logicalId(), source(DATA))));
            assertThat(channels.get(1).isOpen()).isTrue();
            assertThat(channels.get(0).isOpen()).isFalse();
            assertThat(Files.readAllBytes(root.resolve(file.storageKey()))).containsExactly(DATA);
            fails(Failure.CONTEXT_CONSUMED, () -> storage.prepare(context, Map.of()));
        } finally {
            for (var channel : channels) channel.close();
        }
    }
}
