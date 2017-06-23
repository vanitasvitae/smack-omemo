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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.provider.JingleS5BTransportProvider;

import org.jxmpp.jid.Jid;

/**
 * Manager for Jingle SOCKS5 Bytestream transports (XEP-0261).
 */
public final class JingleS5BTransportManager extends JingleTransportManager<JingleS5BTransport> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JingleS5BTransportManager> INSTANCES = new WeakHashMap<>();

    private List<Bytestream.StreamHost> localStreamHosts = null;
    private List<Bytestream.StreamHost> availableStreamHosts = null;

    private JingleS5BTransportManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentTransportProvider(getNamespace(), new JingleS5BTransportProvider());
    }

    public static JingleS5BTransportManager getInstanceFor(XMPPConnection connection) {
        JingleS5BTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleS5BTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
    }

    @Override
    public JingleTransportSession<JingleS5BTransport> transportSession(JingleSession jingleSession) {
        return new JingleS5BTransportSession(jingleSession);
    }

    private List<Bytestream.StreamHost> queryAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Socks5BytestreamManager s5m = Socks5BytestreamManager.getBytestreamManager(getConnection());
        List<Jid> proxies = s5m.determineProxies();
        return determineStreamHostInfo(proxies);
    }

    private List<Bytestream.StreamHost> queryLocalStreamHosts() {
        return Socks5BytestreamManager.getBytestreamManager(getConnection())
                .getLocalStreamHost();
    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        if (availableStreamHosts == null) {
            availableStreamHosts = queryAvailableStreamHosts();
        }
        return availableStreamHosts;
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        if (localStreamHosts == null) {
            localStreamHosts = queryLocalStreamHosts();
        }
        return localStreamHosts;
    }

    public List<Bytestream.StreamHost> determineStreamHostInfo(List<Jid> proxies) {
        XMPPConnection connection = getConnection();
        List<Bytestream.StreamHost> streamHosts = new ArrayList<>();

        Iterator<Jid> iterator = proxies.iterator();
        while (iterator.hasNext()) {
            Jid proxy = iterator.next();
            Bytestream request = new Bytestream();
            request.setType(IQ.Type.get);
            request.setTo(proxy);
            try {
                Bytestream response = connection.createStanzaCollectorAndSend(request).nextResultOrThrow();
                streamHosts.addAll(response.getStreamHosts());
            }
            catch (Exception e) {
                iterator.remove();
            }
        }

        return streamHosts;
    }


    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        if (!resumed) try {
            localStreamHosts = queryLocalStreamHosts();
            availableStreamHosts = queryAvailableStreamHosts();
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LOGGER.log(Level.WARNING, "Could not query available StreamHosts: " + e, e);
        }
    }
}
