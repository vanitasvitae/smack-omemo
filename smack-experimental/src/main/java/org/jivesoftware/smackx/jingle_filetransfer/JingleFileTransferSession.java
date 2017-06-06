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
package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleInputStream;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleTransportInputStreamCallback;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingJingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_ibb.JingleInBandBytestreamTransportManager;
import org.jxmpp.jid.FullJid;

/**
 * Represent a jingle file transfer session.
 */
public class JingleFileTransferSession extends JingleSession {

    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferSession.class.getName());

    private byte[] buffer;

    public JingleFileTransferSession(XMPPConnection connection, FullJid remote, FullJid initiator, FullJid responder, String sid) {
        super(connection, remote, initiator, responder, sid);
    }

    public JingleFileTransferSession(XMPPConnection connection, FullJid remote, FullJid initiator, FullJid responder) {
        super(connection, remote, initiator, responder);
    }

    public JingleFileTransferSession(XMPPConnection connection, Jingle initiate) {
        super(connection, initiate);
    }

    public void setBytes(byte[] bytes) {
        this.buffer = bytes;
    }

    public byte[] getBytes() {
        return buffer;
    }

    @Override
    public void onSessionInitiate(final Jingle jingle) {
        JingleFileTransferManager jfm = JingleFileTransferManager.getInstanceFor(connection);
        jfm.notifyIncomingFileTransferListeners(jingle, new IncomingJingleFileTransferCallback() {
            @Override
            public void acceptFileTransfer(final File target) throws SmackException.NotConnectedException, InterruptedException {
                connection.sendStanza(accept(jingle));
                JingleInBandBytestreamTransportManager.getInstanceFor(connection).acceptInputStream(jingle, new JingleTransportInputStreamCallback() {
                    @Override
                    public void onInputStream(JingleInputStream inputStream) {
                        receive(inputStream, target);
                    }
                });
            }

            @Override
            public void cancelFileTransfer() throws SmackException.NotConnectedException, InterruptedException {
                connection.sendStanza(terminateFormally());
            }
        });
    }

    @Override
    public void onAccept(Jingle jingle) {
        this.contents = jingle.getContents();
        JingleInBandBytestreamTransportManager jibb = JingleInBandBytestreamTransportManager.getInstanceFor(connection);
        OutputStream outputStream = jibb.createOutputStream(jingle);

        if (outputStream == null) {
            LOGGER.log(Level.SEVERE, "OutputStream is null!");
            return;
        }
        send(outputStream);
    }

    void send(OutputStream outputStream) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Caught exception while writing to output stream: " + e, e);
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close output stream: " + e, e);
            }
        }
    }

    void receive(JingleInputStream in, File file) {
        JingleFileTransferChildElement payload = (JingleFileTransferChildElement) contents.get(0).getDescription().getJingleContentDescriptionChildren().get(0);
        InputStream inputStream = in.getInputStream();
        byte[] fileBuffer = new byte[payload.getSize()];
        byte[] packetBuffer = new byte[in.getBlockSize()];

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0, count = 0;
            while (read > -1 && read < fileBuffer.length) {
                int r = inputStream.read(packetBuffer);
                read += r;
                System.arraycopy(packetBuffer, 0, fileBuffer, packetBuffer.length * count, r);
                count++;
            }

            inputStream.close();
            outputStream.write(fileBuffer);
            outputStream.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Caught exception while receiving and writing file: " + e, e);
        }
    }

    @Override
    public void onTerminate(Jingle jingle) {

    }
}
