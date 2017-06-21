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
package org.jivesoftware.smackx.jingle.transports;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

import org.jxmpp.jid.FullJid;

/**
 * Manager for a JingleTransport method.
 * @param <D> JingleContentTransport.
 */
public abstract class JingleTransportManager<D extends JingleContentTransport> {

    private final XMPPConnection connection;

    public JingleTransportManager(XMPPConnection connection) {
        this.connection = connection;
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public abstract String getNamespace();

    public abstract D createTransport(FullJid recipient);

    public abstract D createTransport(Jingle request);

    public abstract void initiateOutgoingSession(FullJid remote, JingleContentTransport transport, JingleTransportInitiationCallback callback);

    public abstract void initiateIncomingSession(FullJid remote, JingleContentTransport transport, JingleTransportInitiationCallback callback);

}
