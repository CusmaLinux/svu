package co.edu.itp.svu.repository;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Process-local proof for a single operation/fence. Only its derived hash can reach MongoDB. */
@com.fasterxml.jackson.annotation.JsonIgnoreType
public final class AttachmentWriterCapability {

    private static final SecureRandom RANDOM = new SecureRandom();

    @com.fasterxml.jackson.annotation.JsonIgnore
    private final byte[] secret = new byte[32];

    private final UUID operation;
    private final long generation;
    private List<UUID> keys;
    private boolean writeAuthorized;

    public AttachmentWriterCapability(UUID operation, long generation) {
        if (generation < 1) throw new IllegalArgumentException("Invalid physical generation");
        this.operation = java.util.Objects.requireNonNull(operation);
        this.generation = generation;
        RANDOM.nextBytes(secret);
    }

    String proof(String operationId, long fencing) {
        if (!operation.toString().equals(operationId) || generation != fencing) {
            throw new IllegalArgumentException("Writer capability belongs to another reservation");
        }
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update("svu/attachment/writer/v2".getBytes(StandardCharsets.UTF_8));
            hash.update(
                ByteBuffer.allocate(24)
                    .putLong(operation.getMostSignificantBits())
                    .putLong(operation.getLeastSignificantBits())
                    .putLong(generation)
                    .array()
            );
            return HexFormat.of().formatHex(hash.digest(secret));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Writer proof unavailable");
        }
    }

    synchronized List<UUID> prepareKeys(int files) {
        if (keys == null) keys = java.util.stream.IntStream.range(0, files).mapToObj(i -> UUID.randomUUID()).toList();
        if (keys.size() != files) throw new IllegalStateException("Physical reservation changed");
        return keys;
    }

    synchronized boolean matchesKeys(List<UUID> persistedKeys) {
        return keys != null && keys.equals(persistedKeys);
    }

    synchronized boolean writeAuthorized() {
        return writeAuthorized;
    }

    synchronized void authorizeWrite() {
        if (writeAuthorized) throw new IllegalStateException("Writer authorization already consumed");
        writeAuthorized = true;
    }

    @Override
    public String toString() {
        return "AttachmentWriterCapability[redacted]";
    }
}
