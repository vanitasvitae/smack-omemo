/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer;

import org.jivesoftware.smackx.hash.HashManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;

/**
 * Dirty.
 */
public final class DirtyHelper {

    public static BytesAndDigest readFile(File file, HashManager.ALGORITHM algorithm) throws IOException {
        byte[] bytes = null;
        byte[] digest = null;
        int read;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            DigestInputStream din = new DigestInputStream(fin, HashManager.getMessageDigest(algorithm));
            bytes = new byte[(int) file.length()];
            read = din.read(bytes);
            din.close();
            digest = din.getMessageDigest().digest();
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
        if (read == -1) {
            return null;
        }

        return new BytesAndDigest(bytes, digest);
    }

    public static class BytesAndDigest {

        private final byte[] bytes, digest;

        public BytesAndDigest(byte[] bytes, byte[] digest) {
            this.bytes = bytes;
            this.digest = digest;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public byte[] getDigest() {
            return digest;
        }
    }
}
