/**
 *
 * Copyright 2018 Paul Schaub.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
