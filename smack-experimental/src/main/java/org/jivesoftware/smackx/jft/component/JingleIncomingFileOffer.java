/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jft.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.component.file.RemoteFile;
import org.jivesoftware.smackx.jft.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Behind the scenes logic of an incoming Jingle file offer.
 * Created by vanitas on 26.07.17.
 */
public class JingleIncomingFileOffer extends AbstractJingleFileOffer<RemoteFile> implements IncomingFileOfferController {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());

    private File target;

    public JingleIncomingFileOffer(JingleFileTransferChildElement offer) {
        super(new RemoteFile(offer));
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        LOGGER.log(Level.INFO, "Receive file to " + target.getAbsolutePath());
        File mFile = target;
        if (!mFile.exists()) {
            try {
                mFile.createNewFile();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not create new File!");
            }
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = bytestreamSession.getInputStream();
            outputStream = new FileOutputStream(mFile);

            int length = 0;
            int read = 0;
            byte[] bufbuf = new byte[4096];
            while ((length = inputStream.read(bufbuf)) >= 0) {
                outputStream.write(bufbuf, 0, length);
                read += length;
                LOGGER.log(Level.INFO, "Read " + read + " (" + length + ") of " + file.getSize() + " bytes.");
                if (read == (int) file.getSize()) {
                    break;
                }
            }
            LOGGER.log(Level.INFO, "Reading/Writing finished.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get InputStream from BytestreamSession: " + e, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                    LOGGER.log(Level.INFO, "CipherInputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close InputStream: " + e, e);
                }
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                    LOGGER.log(Level.INFO, "FileOutputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close OutputStream: " + e, e);
                }
            }
        }
        notifyProgressListenersFinished();
    }

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    @Override
    public Future<Void> accept(XMPPConnection connection, File target)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {
        this.target = target;
        JingleSession session = getParent().getParent();
        if (session.getSessionState() == JingleSession.SessionState.pending) {
            session.sendAccept(connection);
        }

        return null;
    }

    @Override
    public RemoteFile getFile() {
        return (RemoteFile) this.file;
    }
}
