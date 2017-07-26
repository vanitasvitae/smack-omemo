package org.jivesoftware.smackx.jft.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleIncomingFileOffer extends JingleFileOffer<RemoteFile> {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());

    public JingleIncomingFileOffer(JingleFileTransferChildElement offer) {
        super(new RemoteFile(offer));
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        OutputStream outputStream;
        try {
            outputStream = bytestreamSession.getOutputStream();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get OutputStream from BytestreamSession: " + e, e);
            return;
        }
    }
}
