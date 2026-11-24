package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.config.ApplicationProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Physical storage only. Callers must authorize the document before opening or removing it. */
@Component
public class AttachmentStorage {

    public enum Failure {
        INVALID_KEY,
        MISSING,
        UNSAFE_ENTRY,
        UNAVAILABLE,
        WRITE_FAILED,
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

    private final Path root;
    private final Supplier<UUID> keys;

    @Autowired
    public AttachmentStorage(ApplicationProperties properties) {
        this(Path.of(properties.getFile().getUploadDir()), UUID::randomUUID);
    }

    AttachmentStorage(Path root, Supplier<UUID> keys) {
        if (!root.isAbsolute() || root.getNameCount() == 0 || !root.equals(root.normalize())) {
            throw new IllegalArgumentException("Invalid attachment root configuration");
        }
        this.root = root;
        this.keys = keys;
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
