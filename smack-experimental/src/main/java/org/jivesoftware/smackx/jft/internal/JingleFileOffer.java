package org.jivesoftware.smackx.jft.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 22.07.17.
 */
public class JingleFileOffer extends JingleFileTransfer {

    private static final Logger LOGGER = Logger.getLogger(JingleFileOffer.class.getName());

    private File file;

    public JingleFileOffer(File file) {
        super();
        this.file = file;
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        OutputStream outputStream;

        try {
            outputStream = bytestreamSession.getOutputStream();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving outputStream: " + e, e);
            return;
        }


    }
}
