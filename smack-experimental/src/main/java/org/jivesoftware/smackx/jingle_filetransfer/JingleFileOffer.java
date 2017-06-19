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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * Offer.
 */
public class JingleFileOffer extends JingleFileTransferSession {

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
        if (role == Role.initiator) {

        }

        if (getState() != State.fresh) {
            return jutil.createErrorOutOfOrder(initiate);
        }

        IncomingFileOfferCallback callback = new IncomingFileOfferCallback() {
            @Override
            public void accept(JingleFileTransfer file, File target) {

            }

            @Override
            public void decline() {

            }
        };

        JingleFileTransferManager.getInstanceFor(connection).notifyIncomingFileOffer(initiate, callback);
        return jutil.createAck(initiate);
    }
}
