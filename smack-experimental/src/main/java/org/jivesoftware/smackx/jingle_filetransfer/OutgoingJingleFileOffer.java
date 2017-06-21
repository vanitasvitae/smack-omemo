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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * We are the initiator and we are the sender.
 */
public class OutgoingJingleFileOffer extends JingleFileTransferSession {

    private static final Logger LOGGER = Logger.getLogger(OutgoingJingleFileOffer.class.getName());

    public void send(File file) throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        source = file;
        String contentName = JingleManager.randomSid();
        JingleFileTransfer transfer = JingleFileTransferManager.fileTransferFromFile(file);

        initiateFileOffer(transfer, JingleContent.Creator.initiator, contentName);
    }

    public enum State {
        fresh,
        pending,
        sent_transport_replace,
        active,
        terminated,
        ;}

    private Thread sendingThread;
    private File source;
    private State state;


    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid responder, String sid) {
        super(connection, connection.getUser().asFullJidOrThrow(), responder, Role.initiator, sid, Type.offer);
        state = State.fresh;
    }

    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid recipient) {
        this(connection, recipient, JingleManager.randomSid());
    }

    public void initiateFileOffer(JingleFileTransfer file, JingleContent.Creator creator, String name) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        if (state != State.fresh) {
            throw new IllegalStateException("This session is not fresh.");
        }

        transportManager = JingleTransportMethodManager.getInstanceFor(connection)
                .getBestAvailableTransportManager();

        if (transportManager == null) {
            throw new IllegalStateException("There must be at least one workable transport method.");
        }

        transport = transportManager.createTransport(getResponder());

        jutil.sendSessionInitiateFileOffer(getResponder(), getSessionId(), creator, name, file, transport);
        state = State.pending;
    }

    @Override
    public IQ handleSessionAccept(Jingle sessionAccept) throws SmackException.NotConnectedException, InterruptedException {
        // Out of order?
        if (state != State.pending) {
            LOGGER.log(Level.WARNING, "Out of order!");
            return jutil.createErrorOutOfOrder(sessionAccept);
        }

        state = State.active;
        transportManager.initiateOutgoingSession(getResponder(), transport, new JingleTransportInitiationCallback() {
            @Override
            public void onSessionInitiated(final BytestreamSession session) {
                sendingThread = new SendingThread(session, source);
                sendingThread.run();
            }

            @Override
            public void onException(Exception e) {
                LOGGER.log(Level.SEVERE, "Cannot create outgoing Bytestream session: ", e);
            }
        });

        return jutil.createAck(sessionAccept);
    }

    @Override
    public IQ handleSessionTerminate(Jingle sessionTerminate) {

        if (sendingThread != null && !sendingThread.isInterrupted()) {
            sendingThread.interrupt();
        }

        state = State.terminated;
        return jutil.createAck(sessionTerminate);
    }

    @Override
    public IQ handleTransportReplace(Jingle transportReplace)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleTransportManager<?> replacementManager = JingleTransportMethodManager.getInstanceFor(connection)
                .getTransportManager(transportReplace);

        if (replacementManager != null) {
            jutil.sendTransportAccept(transportReplace.getFrom().asFullJidOrThrow(),
                    transportReplace.getInitiator(), transportReplace.getSid(), creator, name,
                    replacementManager.createTransport(getResponder()));
        }

        return jutil.createAck(transportReplace);
    }
}
