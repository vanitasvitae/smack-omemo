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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.Jingle;

import org.jxmpp.jid.FullJid;

/**
 * Request.
 */
public class JingleFileRequest extends JingleFileTransferSession {

    public JingleFileRequest(XMPPConnection connection, FullJid initiator, FullJid responder, Role role, String sid) {
        super(connection, initiator, responder, role, sid, Type.request);
    }

    public static JingleFileRequest createOutgoingFileRequest(XMPPConnection connection, FullJid recipient) {
        return new JingleFileRequest(connection, connection.getUser().asFullJidOrThrow(), recipient, Role.initiator,
                JingleManager.randomSid());
    }

    public static JingleFileRequest createIncomingFileRequest(XMPPConnection connection, Jingle request) {
        return new JingleFileRequest(connection, request.getInitiator(), connection.getUser().asFullJidOrThrow(), Role.responder,
                request.getSid());
    }
}
