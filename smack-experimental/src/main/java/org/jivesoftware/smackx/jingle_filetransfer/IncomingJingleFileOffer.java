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
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.handler.FileTransferHandler;

import org.jxmpp.jid.FullJid;

/**
 * We are the responder and we are the recipient.
 */
public class IncomingJingleFileOffer extends JingleFileTransferSession implements IncomingFileOfferCallback {
    private static final Logger LOGGER = Logger.getLogger(IncomingJingleFileOffer.class.getName());
    private Jingle pendingSessionInitiate = null;
    private ReceiveTask receivingThread;
    private File target;

    @Override
    public void cancel() {
        //TODO: Actually cancel
        notifyEndedListeners(JingleReason.Reason.cancel);
    }

    public enum State {
        fresh,
        pending,
        sent_transport_replace,
        active,
        terminated
    }

    private State state;

    public IncomingJingleFileOffer(XMPPConnection connection, FullJid initiator, String sid) {
        super(connection, initiator, connection.getUser().asFullJidOrThrow(), Role.responder, sid, Type.offer);
        state = State.fresh;
    }

    public IncomingJingleFileOffer(XMPPConnection connection, Jingle request) {
        this(connection, request.getInitiator(), request.getSid());
    }

    @Override
    public IQ handleSessionInitiate(final Jingle initiate)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {
        JingleTransportMethodManager tm = JingleTransportMethodManager.getInstanceFor(connection);

        if (state != State.fresh) {
            //Out of order (initiate after accept)
            LOGGER.log(Level.WARNING, "Action " + initiate.getAction() + " is out of order!");
            return jutil.createErrorOutOfOrder(initiate);
        }

        this.contents.addAll(initiate.getContents());
        this.file = (JingleFileTransfer) contents.get(0).getDescription();

        JingleTransportManager<?> transportManager = tm.getTransportManager(initiate);
        if (transportManager == null) {
            //Try fallback.
            pendingSessionInitiate = initiate;
            transportManager = tm.getBestAvailableTransportManager();

            if (transportManager == null) {
                //No usable transports.
                LOGGER.log(Level.WARNING, "No usable transports.");
                jutil.sendSessionTerminateUnsupportedTransports(getInitiator(), getSessionId());
                state = State.terminated;
                return jutil.createAck(initiate);
            }

            transportSession = transportManager.transportSession(this);
            jutil.sendTransportReplace(initiate.getFrom().asFullJidOrThrow(), getInitiator(),
                    getSessionId(), contents.get(0).getCreator(), contents.get(0).getName(),
                    transportSession.createTransport());
            state = State.sent_transport_replace;
            return jutil.createAck(initiate);
        }

        transportSession = transportManager.transportSession(this);
        transportSession.processJingle(initiate);

        state = State.pending;

        JingleFileTransferManager.getInstanceFor(connection).notifyIncomingFileOffer(initiate,
                IncomingJingleFileOffer.this);

        return jutil.createAck(initiate);
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
                        IncomingJingleFileOffer.this.transportSession = replacementManager.transportSession(IncomingJingleFileOffer.this);
                        transportSession.processJingle(transportReplace);
                        transportSession.initiateIncomingSession(new JingleTransportInitiationCallback() {
                            @Override
                            public void onSessionInitiated(BytestreamSession bytestreamSession) {
                                LOGGER.log(Level.INFO, "Bytestream initiated. Start receiving.");
                                receivingThread = new ReceiveTask(IncomingJingleFileOffer.this, bytestreamSession, file, target);
                                queued.add(JingleManager.getThreadPool().submit(receivingThread));
                            }

                            @Override
                            public void onException(Exception e) {
                                LOGGER.log(Level.SEVERE, "EXCEPTION IN INCOMING SESSION: ", e);
                            }
                        });

                        jutil.sendTransportAccept(transportReplace.getFrom().asFullJidOrThrow(),
                                transportReplace.getInitiator(), transportReplace.getSid(),
                                getContents().get(0).getCreator(), getContents().get(0).getName(),
                                transportSession.createTransport());

                    } else {
                        LOGGER.log(Level.INFO, "Unsupported transport. Reject transport-replace.");
                        jutil.sendTransportReject(transportReplace.getFrom().asFullJidOrThrow(), transportReplace.getInitiator(),
                                transportReplace.getSid(), getContents().get(0).getCreator(),
                                getContents().get(0).getName(), transportReplace.getContents().get(0).getJingleTransport());
                    }
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.SEVERE, "Help me please!", e);
                }
            }
        }));

        return jutil.createAck(transportReplace);
    }

    @Override
    public IQ handleTransportAccept(Jingle transportAccept) {
        LOGGER.log(Level.INFO, "Received transport-accept.");
        if (state != State.sent_transport_replace) {
            LOGGER.log(Level.WARNING, "Session is in state " + state + ", so the transport-accept is out of order.");
            return jutil.createErrorOutOfOrder(transportAccept);
        }

        JingleFileTransferManager.getInstanceFor(connection)
                .notifyIncomingFileOffer(pendingSessionInitiate, this);
        transportSession.processJingle(transportAccept);
        state = State.pending;
        return jutil.createAck(transportAccept);
    }

    @Override
    public void onTransportMethodFailed(String namespace) {
        //Nothing to do.
    }

    @Override
    public FileTransferHandler acceptIncomingFileOffer(final Jingle request, final File target) {
        this.target = target;
        LOGGER.log(Level.INFO, "Client accepted incoming file offer. Try to start receiving.");
        if (transportSession == null) {
            //Unsupported transport
            LOGGER.log(Level.WARNING, "Unsupported Transport method.");
            try {
                jutil.sendSessionTerminateUnsupportedTransports(request.getFrom().asFullJidOrThrow(), sid);
            } catch (InterruptedException | SmackException.NoResponseException |
                    SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
            }
            return null;
        }

        state = State.active;

        try {
            jutil.sendSessionAccept(getInitiator(), sid, getContents().get(0).getCreator(),
                    getContents().get(0).getName(), JingleContent.Senders.initiator, file,
                    transportSession.createTransport());
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException |
                XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Could not send session-accept.", e);
        }

        transportSession.initiateIncomingSession(new JingleTransportInitiationCallback() {
            @Override
            public void onSessionInitiated(BytestreamSession bytestreamSession) {
                LOGGER.log(Level.INFO, "Bytestream initiated. Start receiving.");
                receivingThread = new ReceiveTask(IncomingJingleFileOffer.this, bytestreamSession, file, target);
                queued.add(JingleManager.getThreadPool().submit(receivingThread));
                started = true;
                notifyStartedListeners();
            }

            @Override
            public void onException(Exception e) {
                LOGGER.log(Level.SEVERE, "EXCEPTION IN INCOMING SESSION: ", e);
            }
        });

        return this;
    }

    @Override
    public void declineIncomingFileOffer(Jingle request) {
        state = State.terminated;
        try {
            jutil.sendSessionTerminateDecline(request.getInitiator(), request.getSid());
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException |
                XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
        }
    }
}
