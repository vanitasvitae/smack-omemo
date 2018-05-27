package org.jivesoftware.smackx.ox;

import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.xml.bind.DatatypeConverter;

public class Util {

    /**
     * Calculate the key id of the OpenPGP key, the given {@link OpenPgpV4Fingerprint} belongs to.
     *
     * @see <a href="https://tools.ietf.org/html/rfc4880#section-12.2"> RFC-4880 §12.2</a>
     * @param fingerprint {@link OpenPgpV4Fingerprint}.
     * @return key id
     */
    public static long keyIdFromFingerprint(OpenPgpV4Fingerprint fingerprint) {
        byte[] bytes = DatatypeConverter.parseHexBinary(fingerprint.toString());
        byte[] lower8Bytes = Arrays.copyOfRange(bytes, 12, 20);
        ByteBuffer byteBuffer = ByteBuffer.allocate(8);
        byteBuffer.put(lower8Bytes);
        byteBuffer.flip();
        return byteBuffer.getLong();
    }
}
