package org.jivesoftware.smackx.jft.internal;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;

/**
 * Created by vanitas on 22.07.17.
 */
public class JingleFileRequest extends JingleFileTransfer {

    @Override
    public JingleFileTransferElement getElement() {
        return null;
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {

    }
}
