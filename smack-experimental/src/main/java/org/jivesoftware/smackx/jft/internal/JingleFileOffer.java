package org.jivesoftware.smackx.jft.internal;

import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;

/**
 * Created by vanitas on 22.07.17.
 */
public abstract class JingleFileOffer<D extends JingleFileTransferFile> extends JingleFileTransfer {

    protected D jingleFile;

    public JingleFileOffer(D fileTransferFile) {
        super();
        this.jingleFile = fileTransferFile;
    }

    @Override
    public JingleFileTransferElement getElement() {
        return new JingleFileTransferElement(jingleFile.getElement());
    }
}
