/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.handler;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.exception.UnsupportedJingleTransportException;
import org.jivesoftware.smackx.jingle_filetransfer.FileAndHashReader;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;

/**
 * This handler represents the state of the initiators jingle session after session-initiate was sent.
 */
public class InitiatorOutgoingFileTransferInitiated implements JingleSessionHandler {

    private final WeakReference<JingleFileTransferManager> manager;
    private final JingleManager.FullJidAndSessionId fullJidAndSessionId;
    private final File file;

    public InitiatorOutgoingFileTransferInitiated(JingleFileTransferManager manager, JingleManager.FullJidAndSessionId fullJidAndSessionId, File file) {
        this.fullJidAndSessionId = fullJidAndSessionId;
        this.file = file;
        this.manager = new WeakReference<>(manager);
    }

    @Override
    public IQ handleJingleSessionRequest(final Jingle jingle, String sessionId) {
        final AbstractJingleTransportManager<?> bm;
        try {
            bm = JingleTransportManager.getInstanceFor(getConnection())
                    .getJingleContentTransportManager(jingle);
        } catch (UnsupportedJingleTransportException e) {
            // TODO
            return null;
        }

        switch (jingle.getAction()) {
            case session_accept:

                Runnable transfer = new Runnable() {
                    @Override
                    public void run() {
                        startTransfer(bm, jingle);
                    }
                };
                transfer.run();

                break;
            case session_terminate:
                break;
                default:
                    break;

        }
        return IQ.createResultIQ(jingle);
    }

    @Override
    public XMPPConnection getConnection() {
        JingleFileTransferManager m = manager.get();
        return m != null ? m.getConnection() : null;
    }

    public void startTransfer(AbstractJingleTransportManager<?> transportManager, Jingle jingle) {
    BytestreamSession session;

        try {
            session = transportManager.outgoingInitiatedSession(jingle);
        } catch (Exception e) {
            //TODO
            return;
        }

        HashElement fileHash;
        byte[] buf = new byte[(int) file.length()];

        try {
            fileHash = FileAndHashReader.readAndCalculateHash(file, buf, HashManager.ALGORITHM.SHA_256);
            session.getOutputStream().write(buf);
            session.close();
        } catch (IOException e) {
            //TODO:
            return;
        }
    }
}
