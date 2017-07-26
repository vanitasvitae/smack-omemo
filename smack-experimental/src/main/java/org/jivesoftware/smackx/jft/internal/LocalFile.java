package org.jivesoftware.smackx.jft.internal;

import java.io.File;
import java.util.Date;

/**
 * Created by vanitas on 26.07.17.
 */
public class LocalFile extends JingleFileTransferFile {

    private File file;

    public LocalFile(File file) {
        super();
        this.file = file;
    }

    @Override
    public Date getDate() {
        return new Date(file.lastModified());
    }

    @Override
    public long getSize() {
        return file.length();
    }

    @Override
    public String getName() {
        String path = file.getAbsolutePath();
        return path.substring(path.lastIndexOf(File.pathSeparatorChar)+1);
    }
}
