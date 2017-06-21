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
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * We are the responder and we are the recipient.
 */
public class IncomingJingleFileOffer extends JingleFileTransferSession implements IncomingFileOfferCallback {
    private static final Logger LOGGER = Logger.getLogger(IncomingJingleFileOffer.class.getName());
    private Jingle pendingSessionInitiate = null;

    public enum State {
        fresh,
        pending,
        sent_transport_replace,
        active,
        terminated,
        ;
    }

    private State state;

    public IncomingJingleFileOffer(XMPPConnection connection, FullJid initiator, String sid) {
        super(connection, initiator, connection.getUser().asFullJidOrThrow(), Role.responder, sid, Type.offer);
    }

    public IncomingJingleFileOffer(XMPPConnection connection, Jingle request) {
        this(connection, request.getInitiator(), request.getSid());
    }

    @Override
    public IQ handleSessionInitiate(Jingle initiate) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        JingleTransportMethodManager tm = JingleTransportMethodManager.getInstanceFor(connection);

        if (state != State.fresh) {
            //Out of order (initiate after accept)
            return jutil.createErrorOutOfOrder(initiate);
        }

        JingleContent content = initiate.getContents().get(0);
        this.creator = content.getCreator();
        this.file = (JingleFileTransfer) content.getDescription();
        this.name = content.getName();
        this.transport = content.getJingleTransport();
        this.transportManager = tm.getTransportManager(initiate);

        if (transportManager == null) {
            //Try fallback.
            pendingSessionInitiate = initiate;
            transportManager = tm.getBestAvailableTransportManager();

            if (transportManager == null) {
                //No usable transports.
                jutil.sendSessionTerminateUnsupportedTransports(initiate.getInitiator(), initiate.getSid());
                state = State.terminated;
                return jutil.createAck(initiate);
            }

            transport = transportManager.createTransport(getInitiator());
            jutil.sendTransportReplace(initiate.getFrom().asFullJidOrThrow(), initiate.getInitiator(),
                    initiate.getSid(), creator, name, transport);
            state = State.sent_transport_replace;
            return jutil.createAck(initiate);
        }

        JingleFileTransferManager.getInstanceFor(connection).notifyIncomingFileOffer(initiate, this);
        state = State.pending;
        return jutil.createAck(initiate);
    }

    @Override
    public IQ handleTransportAccept(Jingle transportAccept) {

        if (state != State.sent_transport_replace) {
            return jutil.createErrorOutOfOrder(transportAccept);
        }

        JingleFileTransferManager.getInstanceFor(connection).notifyIncomingFileOffer(pendingSessionInitiate, this);
        transport = transportAccept.getContents().get(0).getJingleTransport();
        state = State.pending;
        return jutil.createAck(transportAccept);
    }

    @Override
    public void acceptIncomingFileOffer(Jingle request, File target) {

        if (transportManager == null) {
            //Unsupported transport
            LOGGER.log(Level.WARNING, "Unsupported Transport method.");
            try {
                jutil.sendSessionTerminateUnsupportedTransports(request.getFrom().asFullJidOrThrow(), sid);
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
            }
            return;
        }

        JingleContentTransport transport = transportManager.createTransport(request);
        try {
            jutil.sendSessionAccept(getInitiator(), sid, creator, name, JingleContent.Senders.initiator, file, transport);
            state = State.active;
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-accept: " + e, e);
        }
    }

    @Override
    public void declineIncomingFileOffer(Jingle request) {
        state = State.terminated;
        try {
            jutil.sendSessionTerminateDecline(request.getInitiator(), request.getSid());
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
        }
    }
}
