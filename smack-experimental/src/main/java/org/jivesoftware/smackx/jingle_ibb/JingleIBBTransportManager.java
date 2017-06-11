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
package org.jivesoftware.smackx.jingle_ibb;

import java.util.WeakHashMap;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle_ibb.provider.JingleIBBTransportProvider;
import org.jxmpp.jid.FullJid;

/**
 * BytestreamManager for Jingle InBandBytestream Transports.
 */
public final class JingleIBBTransportManager extends AbstractJingleTransportManager<JingleIBBTransport> {

    private static final WeakHashMap<XMPPConnection, JingleIBBTransportManager> INSTANCES = new WeakHashMap<>();

    public static JingleIBBTransportManager getInstanceFor(XMPPConnection connection) {
        JingleIBBTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleIBBTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
    }

    @Override
    public JingleTransportHandler<JingleIBBTransport> createJingleTransportHandler(JingleSessionHandler sessionHandler) {
        return new JingleIBBTransportHandler(sessionHandler);
    }

    @Override
    public JingleIBBTransport createJingleContentTransport(FullJid target) {
        return new JingleIBBTransport();
    }

    @Override
    public JingleIBBTransport createJingleContentTransport(Jingle remotesRequest) throws Exception {
        JingleIBBTransport remotesTransport = (JingleIBBTransport) remotesRequest.getContents().get(0)
                .getJingleTransports().get(0);
        return new JingleIBBTransport(remotesTransport.getBlockSize(), remotesTransport.getSessionId());
    }


    @Override
    protected JingleContentTransportProvider<JingleIBBTransport> createJingleContentTransportProvider() {
        return new JingleIBBTransportProvider();
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE_V1;
    }

}
