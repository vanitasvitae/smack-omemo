package org.jivesoftware.smackx.ox.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DecryptedBytesAndMetadata {

    private final byte[] bytes;
    private final Set<Long> verifiedSignatures;
    private final Long decryptionKey;

    public DecryptedBytesAndMetadata(byte[] bytes, Set<Long> verifiedSignatures, Long decryptionKey) {
        this.bytes = bytes;
        this.verifiedSignatures = verifiedSignatures;
        this.decryptionKey = decryptionKey;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public Long getDecryptionKey() {
        return decryptionKey;
    }

    public Set<Long> getVerifiedSignatures() {
        return new HashSet<>(verifiedSignatures);
    }
}
