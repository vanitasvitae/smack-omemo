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
import org.jivesoftware.smackx.hash.element.HashElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;

/**
 * FileReader that reads a file into a byte array while returning the hash of the file.
 */
public final class FileAndHashReader {

    /**
     * Read a file into a byte array and calculate the hash sum of the bytes that were read on the fly.
     * @param file file to read.
     * @param destination byte array where the file is read into.
     * @param algorithm algorithm used to calculate the hash.
     * @return hashElement containing the hash sum of the read bytes.
     * @throws IOException
     */
    public static HashElement readAndCalculateHash(File file, final byte[] destination, HashManager.ALGORITHM algorithm) throws IOException {
        return readAndCalculateHash(file, destination, 0, algorithm);
    }

    /**
     * Read a file into a byte array and calculate the hash sum of the bytes that were read on the fly.
     * @param file file to read.
     * @param destination byte array where the file is read into.
     * @param offset offset from the beginning of the file.
     * @param algorithm algorithm used to calculate the hash.
     * @return hashElement containing the hash sum of the read bytes.
     * @throws IOException
     */
    public static HashElement readAndCalculateHash(File file, final byte[] destination, int offset, HashManager.ALGORITHM algorithm) throws IOException {
        byte[] hash;
        int read;
        FileInputStream fin = null;
        DigestInputStream din = null;
        try {
            fin = new FileInputStream(file);
            try {
                din = new DigestInputStream(fin, HashManager.getMessageDigest(algorithm));
                read = din.read(destination, offset, destination.length);
                hash = din.getMessageDigest().digest();
            }
            finally {
                if (din != null) {
                    din.close();
                }
            }
        }
        finally {
            if (fin != null) {
                fin.close();
            }
        }

        if (read == -1) {
            return null;
        }

        return HashManager.assembleHashElement(algorithm, hash);
    }
}
