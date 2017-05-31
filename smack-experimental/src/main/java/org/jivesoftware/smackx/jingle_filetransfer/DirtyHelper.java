package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Dirty.
 */
public final class DirtyHelper {

    public static byte[] readFile(File file) throws IOException {
        byte[] bytes = null;
        int read;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            bytes = new byte[(int) file.length()];
            read = fin.read(bytes);
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
        if (read == -1) {
            return null;
        }

        return bytes;
    }
}
