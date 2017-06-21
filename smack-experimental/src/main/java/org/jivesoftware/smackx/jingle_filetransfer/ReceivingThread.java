package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;

/**
 * Created by vanitas on 21.06.17.
 */
public class ReceivingThread extends Thread {
    private static final Logger LOGGER = Logger.getLogger(ReceivingThread.class.getName());

    private final BytestreamSession session;
    private final JingleFileTransfer fileTransfer;
    private final File target;

    public ReceivingThread(BytestreamSession session, JingleFileTransfer fileTransfer, File target) {
        this.session = session;
        this.fileTransfer = fileTransfer;
        this.target = target;
    }

    @Override
    public void run() {
        JingleFileTransferChild transfer = (JingleFileTransferChild) fileTransfer.getJingleContentDescriptionChildren().get(0);
        FileOutputStream outputStream = null;
        InputStream inputStream;

        try {
            outputStream = new FileOutputStream(target);
            inputStream = session.getInputStream();

            byte[] filebuf = new byte[transfer.getSize()];
            int read = 0;
            byte[] bufbuf = new byte[2048];

            while (read < filebuf.length) {
                int r = inputStream.read(bufbuf);
                if (r >= 0) {
                    System.arraycopy(bufbuf, 0, filebuf, read, r);
                    read += r;
                } else {
                    //TODO
                }
            }

            outputStream.write(filebuf);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while receiving data: ", e);
        } finally {
            try {
                session.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close InputStream.", e);
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close FileOutputStream.", e);
                }
            }
        }
    }
}
