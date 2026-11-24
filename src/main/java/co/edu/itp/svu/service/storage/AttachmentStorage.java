package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.config.ApplicationProperties;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Lease;
import co.edu.itp.svu.repository.AttachmentOperationRepository.PhysicalWriteContext;
import co.edu.itp.svu.repository.AttachmentOperationRepository.ReservedFile;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/** Physical storage only. Callers must authorize the document before opening or removing it. */
@Component
public class AttachmentStorage {

    public enum Failure {
        INVALID_KEY,
        MISSING,
        UNSAFE_ENTRY,
        UNAVAILABLE,
        WRITE_FAILED,
        COLLISION,
        INVALID_BATCH,
        CONTEXT_CONSUMED,
        CLAIM_LOST,
        CLAIM_UNAVAILABLE,
        SIZE_MISMATCH,
        DIGEST_MISMATCH,
        READ_FAILED,
        VERIFY_FAILED,
        SYNC_FAILED,
        CLOSE_FAILED,
    }

    public static final class StorageException extends IOException {

        private final Failure failure;

        private StorageException(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }

    private static final int BUFFER_SIZE = 8192;
    private static final long DEFAULT_MULTIPART_BYTES = DataSize.ofMegabytes(50).toBytes();
    private final Path root;
    private final Supplier<UUID> keys;

    private final long maxFileBytes;
    private final long maxBatchBytes;
    private final DurableIo durableIo;

    @Autowired
    public AttachmentStorage(
        ApplicationProperties properties,
        @Value("${spring.servlet.multipart.max-file-size:50MB}") String maxFileSize,
        @Value("${spring.servlet.multipart.max-request-size:50MB}") String maxRequestSize
    ) {
        this(
            Path.of(properties.getFile().getUploadDir()),
            UUID::randomUUID,
            DataSize.parse(maxFileSize).toBytes(),
            DataSize.parse(maxRequestSize).toBytes(),
            new DurableIo()
        );
    }

    AttachmentStorage(Path root, Supplier<UUID> keys) {
        this(root, keys, DEFAULT_MULTIPART_BYTES, DEFAULT_MULTIPART_BYTES, new DurableIo());
    }

    AttachmentStorage(Path root, Supplier<UUID> keys, long maxFileBytes, long maxBatchBytes, DurableIo durableIo) {
        if (!root.isAbsolute() || root.getNameCount() == 0 || !root.equals(root.normalize())) {
            throw new IllegalArgumentException("Invalid attachment root configuration");
        }
        this.root = root;
        this.keys = keys;
        this.maxFileBytes = maxFileBytes < 0 ? Long.MAX_VALUE : maxFileBytes;
        this.maxBatchBytes = maxBatchBytes < 0 ? Long.MAX_VALUE : maxBatchBytes;
        this.durableIo = durableIo;
    }

    /** Returns the exclusive key only after the stream has been written and closed. */
    public String write(InputStream source) throws StorageException {
        try (var directory = openRoot()) {
            for (int attempt = 0; attempt < 3; attempt++) {
                Path key = Path.of(keys.get().toString());
                if (writeNew(directory, key, source)) {
                    return key.toString();
                }
            }
            throw new StorageException(Failure.WRITE_FAILED);
        } catch (StorageException e) {
            throw e;
        } catch (IOException e) {
            throw new StorageException(Failure.UNAVAILABLE);
        }
    }

    public record PreparedFile(String logicalId, String storageKey, long size, String sha256) {}

    /** Construction is restricted to a successful preparation in this storage component. */
    @JsonIgnoreType
    public static final class PreparedGeneration {

        private final Lease lease;
        private final List<PreparedFile> files;

        private PreparedGeneration(Lease lease, List<PreparedFile> files) {
            this.lease = lease;
            this.files = List.copyOf(files);
        }

        public Lease lease() {
            return lease;
        }

        public List<PreparedFile> files() {
            return files;
        }

        @Override
        public String toString() {
            return "PreparedGeneration[redacted]";
        }
    }

    /** Streams are borrowed from the caller. All destination channels are owned here. */
    public PreparedGeneration prepare(PhysicalWriteContext context, Map<String, ? extends InputStream> sources) throws StorageException {
        if (!context.start()) throw new StorageException(Failure.CONTEXT_CONSUMED);
        return new PreparedGeneration(context.lease(), prepareReserved(context.files(), sources, context::current));
    }

    // Separated from proof issuance so the storage boundary can be tested with real or failing channels.
    private List<PreparedFile> prepareReserved(
        List<ReservedFile> files,
        Map<String, ? extends InputStream> sources,
        BooleanSupplier current
    ) throws StorageException {
        var inputs = new java.util.HashMap<>(sources);
        validateBatch(files, inputs);
        requireCurrent(current);
        List<PreparedFile> prepared = new ArrayList<>(files.size());
        try (var directory = openRoot()) {
            var directoryChannel = openDurableChannel(directory, Path.of("."), true);
            try (Closeable closeDirectoryChannel = () -> closeDurableChannel(directoryChannel)) {
                for (ReservedFile file : files) {
                    requireCurrent(current);
                    prepared.add(prepareFile(directory, file, inputs.get(file.logicalId())));
                }
                force(directoryChannel);
            }
        } catch (StorageException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new StorageException(Failure.CLOSE_FAILED);
        }
        requireCurrent(current);
        return List.copyOf(prepared);
    }

    private void validateBatch(List<ReservedFile> files, Map<String, ? extends InputStream> sources) throws StorageException {
        Set<String> usedKeys = new HashSet<>();
        Set<String> logicalIds = new HashSet<>();
        long remaining = maxBatchBytes;
        for (ReservedFile file : files) {
            if (!canonicalUuid(file.storageKey()) || !usedKeys.add(file.storageKey())) throw new StorageException(Failure.INVALID_KEY);
            if (
                !canonicalUuid(file.logicalId()) ||
                !logicalIds.add(file.logicalId()) ||
                sources.get(file.logicalId()) == null ||
                file.expectedSha256() == null ||
                !file.expectedSha256().matches("[0-9a-f]{64}")
            ) {
                throw new StorageException(Failure.INVALID_BATCH);
            }
            if (file.expectedSize() < 0 || file.expectedSize() > maxFileBytes || file.expectedSize() > remaining) {
                throw new StorageException(Failure.SIZE_MISMATCH);
            }
            remaining -= file.expectedSize();
        }
        if (!logicalIds.equals(sources.keySet())) throw new StorageException(Failure.INVALID_BATCH);
    }

    private static boolean canonicalUuid(String key) {
        if (key == null || key.length() != 36) return false;
        try {
            return key.equals(UUID.fromString(key).toString());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static void requireCurrent(BooleanSupplier current) throws StorageException {
        boolean valid;
        try {
            valid = current.getAsBoolean();
        } catch (RuntimeException ex) {
            throw new StorageException(Failure.CLAIM_UNAVAILABLE);
        }
        if (!valid) throw new StorageException(Failure.CLAIM_LOST);
    }

    private PreparedFile prepareFile(SecureDirectoryStream<Path> directory, ReservedFile file, InputStream source) throws StorageException {
        var channel = openDurableChannel(directory, Path.of(file.storageKey()), false);
        try (Closeable closeFile = () -> closeDurableChannel(channel)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            MessageDigest digest = sha256();
            long written = 0;
            int read;
            while ((read = readSource(source, buffer)) != -1) {
                if (read > file.expectedSize() - written) throw new StorageException(Failure.SIZE_MISMATCH);
                writeFully(channel, ByteBuffer.wrap(buffer, 0, read));
                digest.update(buffer, 0, read);
                written += read;
            }
            if (written != file.expectedSize()) throw new StorageException(Failure.SIZE_MISMATCH);
            String observed = HexFormat.of().formatHex(digest.digest());
            if (!observed.equals(file.expectedSha256())) throw new StorageException(Failure.DIGEST_MISMATCH);
            force(channel);
            verifyWritten(channel, buffer, written, observed);
            return new PreparedFile(file.logicalId(), file.storageKey(), written, observed);
        } catch (StorageException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new StorageException(Failure.CLOSE_FAILED);
        }
    }

    private static int readSource(InputStream source, byte[] buffer) throws StorageException {
        try {
            int count = source.read(buffer);
            if (count != 0) return count;
            int next = source.read();
            if (next < 0) return -1;
            buffer[0] = (byte) next;
            return 1;
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(Failure.READ_FAILED);
        }
    }

    private void writeFully(FileChannel channel, ByteBuffer buffer) throws StorageException {
        try {
            while (buffer.hasRemaining()) {
                if (channel.write(buffer) <= 0) throw new StorageException(Failure.WRITE_FAILED);
            }
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(Failure.WRITE_FAILED);
        }
    }

    private static void verifyWritten(FileChannel channel, byte[] buffer, long expectedSize, String expectedDigest)
        throws StorageException {
        try {
            if (channel.size() != expectedSize) throw new StorageException(Failure.VERIFY_FAILED);
            MessageDigest digest = sha256();
            channel.position(0);
            ByteBuffer bytes = ByteBuffer.wrap(buffer);
            long remaining = expectedSize;
            while (remaining > 0) {
                bytes.clear().limit((int) Math.min(bytes.capacity(), remaining));
                int read = channel.read(bytes);
                if (read <= 0) throw new StorageException(Failure.VERIFY_FAILED);
                digest.update(buffer, 0, read);
                remaining -= read;
            }
            if (!HexFormat.of().formatHex(digest.digest()).equals(expectedDigest)) throw new StorageException(Failure.VERIFY_FAILED);
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(Failure.VERIFY_FAILED);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable");
        }
    }

    private FileChannel openDurableChannel(SecureDirectoryStream<Path> directory, Path key, boolean directoryEntry)
        throws StorageException {
        try {
            SeekableByteChannel channel = durableIo.open(directory, key, directoryEntry);
            if (channel instanceof FileChannel fileChannel) return fileChannel;
            closeDurableChannel(channel);
            throw new StorageException(Failure.UNAVAILABLE);
        } catch (StorageException ex) {
            throw ex;
        } catch (FileAlreadyExistsException ex) {
            throw new StorageException(Failure.COLLISION);
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(directoryEntry ? Failure.UNAVAILABLE : Failure.WRITE_FAILED);
        }
    }

    private void force(FileChannel channel) throws StorageException {
        try {
            durableIo.force(channel);
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(Failure.SYNC_FAILED);
        }
    }

    private void closeDurableChannel(SeekableByteChannel channel) throws StorageException {
        try {
            durableIo.close(channel);
        } catch (IOException | RuntimeException ex) {
            throw new StorageException(Failure.CLOSE_FAILED);
        }
    }

    static class DurableIo {

        SeekableByteChannel open(SecureDirectoryStream<Path> directory, Path key, boolean directoryEntry) throws IOException {
            if (directoryEntry) return directory.newByteChannel(key, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
            return directory.newByteChannel(
                key,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            );
        }

        void force(FileChannel channel) throws IOException {
            channel.force(true);
        }

        void close(SeekableByteChannel channel) throws IOException {
            channel.close();
        }
    }

    private boolean writeNew(SecureDirectoryStream<Path> directory, Path key, InputStream source) throws StorageException {
        try {
            var channel = directory.newByteChannel(
                key,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            );
            try (var destination = Channels.newOutputStream(channel)) {
                source.transferTo(destination);
            } catch (IOException | RuntimeException e) {
                try {
                    directory.deleteFile(key);
                } catch (IOException cleanupFailure) {
                    var failure = new StorageException(Failure.WRITE_FAILED);
                    failure.addSuppressed(new StorageException(Failure.UNAVAILABLE));
                    throw failure;
                }
                throw new StorageException(Failure.WRITE_FAILED);
            }
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        } catch (IOException e) {
            if (e instanceof StorageException failure) {
                throw failure;
            }
            throw new StorageException(Failure.WRITE_FAILED);
        }
    }

    public InputStream open(String storageKey) throws StorageException {
        Path key = keyPath(storageKey);
        try (var directory = openRoot()) {
            requireRegularFile(directory, key);
            return Channels.newInputStream(directory.newByteChannel(key, Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)));
        } catch (StorageException e) {
            throw e;
        } catch (NoSuchFileException e) {
            throw new StorageException(Failure.MISSING);
        } catch (IOException e) {
            throw new StorageException(Failure.UNAVAILABLE);
        }
    }

    public void delete(String storageKey) throws StorageException {
        Path key = keyPath(storageKey);
        try (var directory = openRoot()) {
            requireRegularFile(directory, key);
            directory.deleteFile(key);
        } catch (StorageException e) {
            throw e;
        } catch (NoSuchFileException e) {
            throw new StorageException(Failure.MISSING);
        } catch (IOException e) {
            throw new StorageException(Failure.UNAVAILABLE);
        }
    }

    private void requireRegularFile(SecureDirectoryStream<Path> directory, Path key) throws IOException {
        var attributes = directory.getFileAttributeView(key, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).readAttributes();
        if (!attributes.isRegularFile()) {
            throw new StorageException(Failure.UNSAFE_ENTRY);
        }
    }

    private Path keyPath(String key) throws StorageException {
        if (
            key == null ||
            key.isBlank() ||
            key.equals(".") ||
            key.equals("..") ||
            key.indexOf('/') >= 0 ||
            key.indexOf('\\') >= 0 ||
            key.indexOf(':') >= 0 ||
            key.codePoints().anyMatch(Character::isISOControl)
        ) {
            throw new StorageException(Failure.INVALID_KEY);
        }
        try {
            return Path.of(key);
        } catch (InvalidPathException e) {
            throw new StorageException(Failure.INVALID_KEY);
        }
    }

    private SecureDirectoryStream<Path> openRoot() throws StorageException {
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(root.getRoot());
            if (!(stream instanceof SecureDirectoryStream<Path> secure)) {
                stream.close();
                throw new StorageException(Failure.UNAVAILABLE);
            }
            var current = secure;
            for (Path part : root) {
                try (var parent = current) {
                    current = parent.newDirectoryStream(part, LinkOption.NOFOLLOW_LINKS);
                }
            }
            return current;
        } catch (IOException e) {
            throw new StorageException(Failure.UNAVAILABLE);
        }
    }
}
