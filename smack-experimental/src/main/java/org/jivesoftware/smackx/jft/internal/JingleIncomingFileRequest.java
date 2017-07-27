package org.jivesoftware.smackx.jft.internal;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.IncomingFileRequestController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;

/**
 * Created by vanitas on 27.07.17.
 */
public class JingleIncomingFileRequest extends AbstractJingleFileRequest implements IncomingFileRequestController {
    @Override
    public JingleFileTransferElement getElement() {
        return null;
    }

    @Override
    public boolean isOffer() {
        return false;
    }

    @Override
    public boolean isRequest() {
        return true;
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {

    }
}
