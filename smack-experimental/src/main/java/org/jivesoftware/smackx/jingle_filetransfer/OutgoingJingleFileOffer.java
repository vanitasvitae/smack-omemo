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

import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * We are the initiator and we are the sender.
 */
public class OutgoingJingleFileOffer extends JingleFileTransferSession {

    private static final Logger LOGGER = Logger.getLogger(OutgoingJingleFileOffer.class.getName());

    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid responder, String sid) {
        super(connection, connection.getUser().asFullJidOrThrow(), responder, Role.initiator, sid, Type.offer);
    }

    public OutgoingJingleFileOffer(XMPPConnection connection, FullJid recipient) {
        this(connection, recipient, JingleManager.randomSid());
    }

    public void sendFile(JingleFileTransfer file, JingleContent.Creator creator, String name) throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        if (getState() == State.fresh) {
            JingleTransportManager<?> transportManager = JingleTransportMethodManager.getInstanceFor(connection)
                    .getBestAvailableTransportManager();

            if (transportManager == null) {
                throw new IllegalStateException("There must be at least one workable transport method.");
            }

            JingleContentTransport transport = transportManager.createTransport();

            jutil.sendSessionInitiateFileOffer(getResponder(), getSessionId(), creator, name, file, transport);
        }
    }
}
