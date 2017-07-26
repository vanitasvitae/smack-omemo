package org.jivesoftware.smackx.jft.internal;

import java.util.Date;

import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class RemoteFile extends JingleFileTransferFile {

    private JingleFileTransferChildElement file;

    public RemoteFile(JingleFileTransferChildElement file) {
        super();
        this.file = file;
    }

    @Override
    public Date getDate() {
        return file.getDate();
    }

    @Override
    public long getSize() {
        return file.getSize();
    }

    @Override
    public String getName() {
        return file.getName();
    }
}
