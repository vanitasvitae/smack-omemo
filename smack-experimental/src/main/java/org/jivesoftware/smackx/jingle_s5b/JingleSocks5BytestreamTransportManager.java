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
package org.jivesoftware.smackx.jingle_s5b;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.jingle.JingleBytestreamManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleSocks5BytestreamTransportProvider;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * Manager for JingleSocks5BytestreamTransports.
 */
public final class JingleSocks5BytestreamTransportManager extends JingleBytestreamManager<JingleSocks5BytestreamTransport> {

    private static final WeakHashMap<XMPPConnection, JingleSocks5BytestreamTransportManager> INSTANCES = new WeakHashMap<>();

    private JingleSocks5BytestreamTransportManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleSocks5BytestreamTransportManager getInstanceFor(XMPPConnection connection) {
        JingleSocks5BytestreamTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleSocks5BytestreamTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Socks5BytestreamManager s5m = Socks5BytestreamManager.getBytestreamManager(connection());
        List<Jid> proxies = s5m.determineProxies();
        return determineStreamHostInfos(proxies);
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        return Socks5BytestreamManager.getBytestreamManager(connection())
                .getLocalStreamHost();
    }

    public List<Bytestream.StreamHost> determineStreamHostInfos(List<Jid> proxies) {
        XMPPConnection connection = connection();
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

    public void connectToStreamHost() {

    }


    @Override
    protected JingleContentTransportProvider<JingleSocks5BytestreamTransport> createJingleContentTransportProvider() {
        return new JingleSocks5BytestreamTransportProvider();
    }

    @Override
    public String getNamespace() {
        return JingleSocks5BytestreamTransport.NAMESPACE_V1;
    }

    @Override
    public Jingle createSessionInitiate(FullJid targetJID, JingleContentDescription application, String sessionId) throws XMPPException, IOException, InterruptedException, SmackException {
        return null;
    }

    @Override
    public Jingle createSessionAccept(Jingle request) {
        return null;
    }

    @Override
    public BytestreamSession outgoingInitiatedSession(Jingle jingle) throws Exception {
        return null;
    }

    @Override
    public void setIncomingRespondedSessionListener(Jingle jingle, BytestreamListener listener) {

    }

    public JingleSocks5BytestreamTransport createJingleContentTransport(Jid remote, JingleContentTransport received_) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        JingleSocks5BytestreamTransport received = (JingleSocks5BytestreamTransport) received_;

        JingleSocks5BytestreamTransport.Builder builder = JingleSocks5BytestreamTransport.getBuilder();
        List<Bytestream.StreamHost> localStreams = getLocalStreamHosts();
        List<Bytestream.StreamHost> availableStreams = getAvailableStreamHosts();

        for (Bytestream.StreamHost host : localStreams) {
            JingleSocks5BytestreamTransportCandidate candidate = new JingleSocks5BytestreamTransportCandidate(host, 100);
            builder.addTransportCandidate(candidate);
        }

        for (Bytestream.StreamHost host : availableStreams) {
            JingleSocks5BytestreamTransportCandidate candidate = new JingleSocks5BytestreamTransportCandidate(host, 0);
            builder.addTransportCandidate(candidate);
        }

        String sid = (received == null ? JingleTransportManager.generateRandomId() : received.getStreamId());
        builder.setStreamId(sid);
        builder.setMode(received == null ? Bytestream.Mode.tcp : received.getMode());

        String digestString =
                        sid +
                        connection().getUser().asFullJidIfPossible().toString() +
                        remote.asFullJidIfPossible().toString();
        builder.setDestinationAddress(HashManager.sha_1HexString(digestString));
        return builder.build();
    }

    public static class Session {
        private ArrayList<Bytestream.StreamHost> ourStreamHosts;
        private ArrayList<Bytestream.StreamHost> theirStreamHosts;


    }

}
