package org.jivesoftware.smackx.jft.internal.file;

import java.util.Date;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class RemoteFile extends AbstractJingleFileTransferFile {

    private JingleFileTransferChildElement file;

    public RemoteFile(JingleFileTransferChildElement file) {
        super();
        this.file = file;
    }

    @Override
    public String getDescription() {
        return file.getDescription();
    }

    @Override
    public String getMediaType() {
        return file.getMediaType();
    }

    @Override
    public HashElement getHashElement() {
        return file.getHash();
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
