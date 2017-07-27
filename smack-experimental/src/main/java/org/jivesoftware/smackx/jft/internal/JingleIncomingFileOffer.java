package org.jivesoftware.smackx.jft.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jft.internal.file.RemoteFile;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleIncomingFileOffer extends AbstractJingleFileOffer<RemoteFile> implements IncomingFileOfferController {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());

    public JingleIncomingFileOffer(JingleFileTransferChildElement offer) {
        super(new RemoteFile(offer));
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        InputStream inputStream;
        try {
            inputStream = bytestreamSession.getInputStream();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get InputStream from BytestreamSession: " + e, e);
            return;
        }
    }

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }
}
