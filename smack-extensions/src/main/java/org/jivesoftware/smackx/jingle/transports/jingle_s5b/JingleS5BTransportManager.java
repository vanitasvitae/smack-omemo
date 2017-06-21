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
import java.util.Collections;
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
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.provider.JingleS5BTransportProvider;

import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * Manager for Jingle SOCKS5 Bytestream transports (XEP-0261).
 */
public final class JingleS5BTransportManager extends JingleTransportManager<JingleS5BTransport> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JingleS5BTransportManager> INSTANCES = new WeakHashMap<>();

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
    public JingleS5BTransport createTransport(FullJid recipient) {
        return createTransport(recipient, JingleManager.randomSid(), Bytestream.Mode.tcp);
    }

    @Override
    public JingleS5BTransport createTransport(Jingle request) {
        FullJid remote = request.getFrom().asFullJidIfPossible();
        JingleS5BTransport received = (JingleS5BTransport) request.getContents().get(0).getJingleTransport();

        return createTransport(remote, received.getStreamId(), received.getMode());
    }

    private JingleS5BTransport createTransport(FullJid remote, String sid, Bytestream.Mode mode) {
        JingleS5BTransport.Builder builder = JingleS5BTransport.getBuilder();
        List<Bytestream.StreamHost> localStreams = getLocalStreamHosts();
        List<Bytestream.StreamHost> availableStreams;
        try {
            availableStreams = getAvailableStreamHosts();
        } catch (XMPPException.XMPPErrorException | SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException e) {
            LOGGER.log(Level.WARNING, "Could not determine available StreamHosts: ", e);
            availableStreams = Collections.emptyList();
        }

        for (Bytestream.StreamHost host : localStreams) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 100);
            builder.addTransportCandidate(candidate);
        }

        for (Bytestream.StreamHost host : availableStreams) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 0);
            builder.addTransportCandidate(candidate);
        }

        builder.setStreamId(sid);
        builder.setMode(mode);
        builder.setDestinationAddress(Socks5Utils.createDigest(sid, getConnection().getUser().asFullJidOrThrow(), remote));
        return builder.build();
    }


    @Override
    public void initiateOutgoingSession(FullJid remote, JingleContentTransport transport, JingleTransportInitiationCallback callback) {

    }

    @Override
    public void initiateIncomingSession(FullJid remote, JingleContentTransport transport, JingleTransportInitiationCallback callback) {

    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Socks5BytestreamManager s5m = Socks5BytestreamManager.getBytestreamManager(getConnection());
        List<Jid> proxies = s5m.determineProxies();
        return determineStreamHostInfo(proxies);
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        return Socks5BytestreamManager.getBytestreamManager(getConnection())
                .getLocalStreamHost();
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
}
