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
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileRequestCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * Offer.
 */
public class JingleFileOffer extends JingleFileTransferSession implements IncomingFileOfferCallback, IncomingFileRequestCallback {

    private static final Logger LOGGER = Logger.getLogger(JingleFileOffer.class.getName());

    public JingleFileOffer(XMPPConnection connection, FullJid initiator, FullJid responder, Role role, String sid) {
        super(connection, initiator, responder, role, sid, Type.offer);
    }

    public static JingleFileOffer createOutgoingFileOffer(XMPPConnection connection, FullJid recipient) {
        return new JingleFileOffer(connection, connection.getUser().asFullJidOrThrow(), recipient,
                Role.initiator, JingleManager.randomSid());
    }

    public static JingleFileOffer createIncomingFileOffer(XMPPConnection connection, Jingle request) {
        return new JingleFileOffer(connection, request.getInitiator(), connection.getUser().asFullJidOrThrow(),
                Role.responder, request.getSid());
    }

    @Override
    public IQ handleSessionInitiate(Jingle initiate) {
        setState(State.pending);
        if (role == Role.initiator) {
            //TODO: Illegal stanza. Figure out, if we handle it correct.
            return jutil.createErrorTieBreak(initiate);
        }

        if (getState() != State.fresh) {
            return jutil.createErrorOutOfOrder(initiate);
        }

        JingleFileTransferManager.getInstanceFor(connection).notifyIncomingFileOffer(initiate, this);
        return jutil.createAck(initiate);
    }

    @Override
    public void acceptIncomingFileOffer(Jingle request, File target) {
        FullJid recipient = request.getInitiator();
        String sid = request.getSid();
        JingleContent content = request.getContents().get(0);

        //Get TransportManager
        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getInstanceFor(connection)
                .getTransportManager(request);

        if (transportManager == null) {
            //Unsupported transport
            LOGGER.log(Level.WARNING, "Unsupported Transport method.");
            setState(State.terminated);
            try {
                jutil.sendSessionTerminateUnsupportedTransports(recipient, sid);
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
            }
            return;
        }

        JingleContentTransport transport = transportManager.createTransport(request);
        try {
            jutil.sendSessionAccept(recipient, sid, content.getCreator(), content.getName(), content.getSenders(),
                    content.getDescription(), transport);
            setState(State.active);
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-accept: " + e, e);
        }
    }

    @Override
    public void declineIncomingFileOffer(Jingle request) {
        setState(State.terminated);
        try {
            jutil.sendSessionTerminateDecline(request.getInitiator(), request.getSid());
        } catch (SmackException.NotConnectedException | SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send session-terminate: " + e, e);
        }
    }

    @Override
    public void acceptIncomingFileRequest(JingleFileTransfer file, File source) {

    }

    @Override
    public void declineIncomingFileRequest() {

    }
}
