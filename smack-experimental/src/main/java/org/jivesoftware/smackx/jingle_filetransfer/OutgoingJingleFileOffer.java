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
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * We are the initiator and we are the sender.
 */
public class OutgoingJingleFileOffer extends JingleFileTransferSession {

    private static final Logger LOGGER = Logger.getLogger(OutgoingJingleFileOffer.class.getName());

    @Override
    public void cancel() throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        switch (state) {
            case terminated:
                return;

            case active:
                Future<?> task = queued.get(0);
                if (task != null) {
                    task.cancel(true);
                    queued.remove(task);
                }
                break;

            default:
        }

        jutil.sendSessionTerminateCancel(getRemote(), getSessionId());
        notifyEndedListeners(JingleReason.Reason.cancel);
    }

    public enum State {
        fresh,
        pending,
        sent_transport_replace,
        active,
        terminated
    }

    private Runnable sendingThread;
    private File source;
    private State state;


    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid responder, String sid) {
        super(connection, connection.getUser().asFullJidOrThrow(), responder, Role.initiator, sid, Type.offer);
        state = State.fresh;
    }

    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid recipient) {
        this(connection, recipient, JingleManager.randomId());
    }

    public void send(File file) throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        source = file;
        String contentName = JingleManager.randomId();
        JingleFileTransfer transfer = JingleFileTransferManager.fileTransferFromFile(file);

        initiateFileOffer(transfer, JingleContent.Creator.initiator, contentName);
    }

    public SmackFuture<?> sendAsync(File file) {
        source = file;
        String contentName = "jft-" + StringUtils.randomString(20);
        JingleFileTransfer transfer = JingleFileTransferManager.fileTransferFromFile(file);
        return null; //TODO
    }

    public void initiateFileOffer(JingleFileTransfer file, JingleContent.Creator creator, String name) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        if (state != State.fresh) {
            throw new IllegalStateException("This session is not fresh.");
        }

        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getInstanceFor(connection)
                .getBestAvailableTransportManager();

        if (transportManager == null) {
            throw new IllegalStateException("There must be at least one workable transport method.");
        }

        transportSession = transportManager.transportSession(this);

        state = State.pending;

        Jingle initiate = jutil.createSessionInitiateFileOffer(getResponder(), getSessionId(), creator, name, file, transportSession.createTransport(), null);
        this.contents.addAll(initiate.getContents());

        connection.sendStanza(initiate);
    }

    @Override
    public IQ handleSessionAccept(Jingle sessionAccept) throws SmackException.NotConnectedException, InterruptedException {
        // Out of order?
        if (state != State.pending) {
            LOGGER.log(Level.WARNING, "Session state is " + state + ", so session-accept is out of order.");
            return jutil.createErrorOutOfOrder(sessionAccept);
        }

        state = State.active;

        transportSession.processJingle(sessionAccept);

        transportSession.initiateOutgoingSession(new JingleTransportInitiationCallback() {
            @Override
            public void onSessionInitiated(final BytestreamSession byteStream) {
                sendingThread = new SendTask(OutgoingJingleFileOffer.this, byteStream, source);
                queued.add(JingleManager.getThreadPool().submit(sendingThread));
                notifyStartedListeners();
            }

            @Override
            public void onException(Exception e) {
                LOGGER.log(Level.SEVERE, "EXCEPTION IN OUTGOING SESSION:", e);
            }
        });

        return jutil.createAck(sessionAccept);
    }

    @Override
    public IQ handleSessionTerminate(Jingle sessionTerminate) {
        state = State.terminated;
        return jutil.createAck(sessionTerminate);
    }

    @Override
    public IQ handleTransportReplace(final Jingle transportReplace)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        final JingleTransportManager<?> replacementManager = JingleTransportMethodManager.getInstanceFor(connection)
                .getTransportManager(transportReplace);

        queued.add(JingleManager.getThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    if (replacementManager != null) {
                        LOGGER.log(Level.INFO, "Accept transport-replace.");
                        jutil.sendTransportAccept(transportReplace.getFrom().asFullJidOrThrow(),
                                transportReplace.getInitiator(), transportReplace.getSid(),
                                getContents().get(0).getCreator(), getContents().get(0).getName(),
                                transportSession.createTransport());
                    } else {
                        LOGGER.log(Level.INFO, "Unsupported transport. Reject transport-replace.");
                        jutil.sendTransportReject(transportReplace.getFrom().asFullJidOrThrow(), transportReplace.getInitiator(),
                                transportReplace.getSid(), getContents().get(0).getCreator(),
                                getContents().get(0).getName(), transportReplace.getContents().get(0).getTransport());
                    }
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Help me please!", e);
                }
            }
        }));

        return jutil.createAck(transportReplace);
    }

    @Override
    public IQ handleTransportAccept(Jingle transportAccept)
            throws SmackException.NotConnectedException, InterruptedException {

        return handleSessionAccept(transportAccept);
    }

    @Override
    public void onTransportMethodFailed(String namespace) {
        state = State.pending;
        JingleContent content = contents.get(0);
        failedTransportMethods.add(namespace);
        JingleTransportMethodManager tm = JingleTransportMethodManager.getInstanceFor(getConnection());
        JingleTransportManager<?> next = tm.getBestAvailableTransportManager(failedTransportMethods);

        if (next == null) {
            //Failure
            try {
                jutil.sendSessionTerminateUnsupportedTransports(getRemote(), getSessionId());
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Could not send session-terminate.", e);
            }
            return;
        }

        //Replace transport
        this.transportSession = next.transportSession(this);
        try {
            jutil.sendTransportReplace(getRemote(), getInitiator(), getSessionId(), content.getCreator(), content.getName(),
                    transportSession.createTransport());
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Could not send transport-replace.", e);
        }
    }
}
