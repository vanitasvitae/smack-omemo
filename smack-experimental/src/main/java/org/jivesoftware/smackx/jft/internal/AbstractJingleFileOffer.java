package org.jivesoftware.smackx.jft.internal;

import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jft.internal.file.AbstractJingleFileTransferFile;

/**
 * Created by vanitas on 22.07.17.
 */
public abstract class AbstractJingleFileOffer<D extends AbstractJingleFileTransferFile> extends AbstractJingleFileTransfer {

    protected D jingleFile;

    public AbstractJingleFileOffer(D fileTransferFile) {
        super();
        this.jingleFile = fileTransferFile;
    }

    @Override
    public JingleFileTransferElement getElement() {
        return new JingleFileTransferElement(jingleFile.getElement());
    }
}
