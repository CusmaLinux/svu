package co.edu.itp.svu.repository;

import java.util.Date;

/** Package-private proof emitted only after manifest, ownership, metadata and physical bytes are rechecked. */
final class VerifiedAttachmentPublication {

    final String collection;
    final Object rootId;
    final String operationId;
    final String publicationId;
    final String digest;
    final long publicationRevision;
    final long manifestRevision;
    final long fencingToken;
    final long selectedGeneration;
    final Date leaseUntil;

    VerifiedAttachmentPublication(
        String collection,
        Object rootId,
        String operationId,
        String publicationId,
        String digest,
        long publicationRevision,
        long manifestRevision,
        long fencingToken,
        long selectedGeneration,
        Date leaseUntil
    ) {
        this.collection = collection;
        this.rootId = rootId;
        this.operationId = operationId;
        this.publicationId = publicationId;
        this.digest = digest;
        this.publicationRevision = publicationRevision;
        this.manifestRevision = manifestRevision;
        this.fencingToken = fencingToken;
        this.selectedGeneration = selectedGeneration;
        this.leaseUntil = new Date(leaseUntil.getTime());
    }
}
