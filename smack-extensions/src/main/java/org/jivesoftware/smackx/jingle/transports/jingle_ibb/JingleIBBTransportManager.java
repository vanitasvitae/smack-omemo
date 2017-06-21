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
package org.jivesoftware.smackx.jingle.transports.jingle_ibb;

import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle InBandBytestream transports (XEP-0261).
 */
public final class JingleIBBTransportManager extends JingleTransportManager<JingleIBBTransport> {

    private static final WeakHashMap<XMPPConnection, JingleIBBTransportManager> INSTANCES = new WeakHashMap<>();

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleIBBTransportManager getInstanceFor(XMPPConnection connection) {
        JingleIBBTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleIBBTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE_V1;
    }

    @Override
    public JingleIBBTransport createTransport() {
        return new JingleIBBTransport();
    }

    @Override
    public JingleIBBTransport createTransport(Jingle request) {
        JingleIBBTransport rTransport = (JingleIBBTransport) request.getContents().get(0).getJingleTransport();
        return new JingleIBBTransport(rTransport.getBlockSize(), rTransport.getSessionId());
    }

    @Override
    public void initiateOutgoingSession(FullJid remote, JingleContentTransport transport, JingleTransportInitiationCallback callback) {
        JingleIBBTransport ibbTransport = (JingleIBBTransport) transport;
        BytestreamSession session;

        try {
            session = InBandBytestreamManager.getByteStreamManager(getConnection())
                    .establishSession(remote, ibbTransport.getSessionId());
            callback.onSessionInitiated(session);
        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            callback.onException(e);
        }
    }

    @Override
    public void initiateIncomingSession(final FullJid remote, final JingleContentTransport transport, final JingleTransportInitiationCallback callback) {
        final JingleIBBTransport ibbTransport = (JingleIBBTransport) transport;

        InBandBytestreamManager.getByteStreamManager(getConnection()).addIncomingBytestreamListener(new BytestreamListener() {
            @Override
            public void incomingBytestreamRequest(BytestreamRequest request) {
                if (request.getFrom().asFullJidIfPossible().equals(remote)
                        && request.getSessionID().equals(ibbTransport.getSessionId())) {

                    BytestreamSession session;

                    try {
                        session = request.accept();
                    } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                        callback.onException(e);
                        return;
                    }
                    callback.onSessionInitiated(session);
                }
            }
        });
    }
}
