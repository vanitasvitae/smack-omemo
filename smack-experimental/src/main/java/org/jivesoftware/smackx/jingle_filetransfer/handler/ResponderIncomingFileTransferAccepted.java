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
import java.lang.ref.WeakReference;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedJingleTransportException;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jxmpp.jid.FullJid;

/**
 * This handler represents the state of the responders jingle session after the responder sent session-accept.
 */
public class ResponderIncomingFileTransferAccepted implements JingleSessionHandler {

    private static final Logger LOGGER = Logger.getLogger(ResponderIncomingFileTransferAccepted.class.getName());

    private final WeakReference<JingleFileTransferManager> manager;
    private AbstractJingleTransportManager<?> transportManager;
    private final File target;
    private final int size;
    private final FullJid initiator;
    private final String sessionId;

    public ResponderIncomingFileTransferAccepted(final JingleFileTransferManager manager, final Jingle initiate, final File target) {
        this.manager = new WeakReference<>(manager);
        this.target = target;
        this.size = ((JingleFileTransferChild) initiate.getContents().get(0).getDescription()
                .getJingleContentDescriptionChildren().get(0)).getSize();
        try {
            this.transportManager = JingleTransportManager.getInstanceFor(manager.getConnection()).getJingleContentTransportManager(initiate);
        } catch (UnsupportedJingleTransportException e) {
            e.printStackTrace();
        }
        this.initiator = initiate.getInitiator();
        this.sessionId = initiate.getSid();

        transportManager.createJingleTransportHandler(this).establishIncomingSession(
                new JingleManager.FullJidAndSessionId(initiate.getFrom().asFullJidIfPossible(), initiate.getSid()),
                initiate.getContents().get(0).getJingleTransports().get(0),
                new JingleTransportEstablishedCallback() {
                    @Override
                    public void onSessionEstablished(BytestreamSession bytestreamSession) {
                        manager.receiveFile(initiate, bytestreamSession, target);
                    }

                    @Override
                    public void onSessionFailure(JingleTransportFailureException reason) {

                    }
                });
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {
        return null;
    }

    @Override
    public XMPPConnection getConnection() {
        JingleFileTransferManager m = manager.get();
        return m != null ? m.getConnection() : null;
    }
}
