package org.jivesoftware.smackx.ox.util;

import java.util.Arrays;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

public class KeyBytesAndFingerprint {

    private final byte[] bytes;
    private final OpenPgpV4Fingerprint fingerprint;

    public KeyBytesAndFingerprint(byte[] bytes, OpenPgpV4Fingerprint fingerprint) {
        this.bytes = bytes;
        this.fingerprint = fingerprint;
    }

    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public OpenPgpV4Fingerprint getFingerprint() {
        return fingerprint;
    }
}
