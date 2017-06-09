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
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleS5BTransportProvider;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * Manager for JingleSocks5BytestreamTransports.
 */
public final class JingleS5BTransportManager extends AbstractJingleTransportManager<JingleS5BTransport> {

    private static final WeakHashMap<XMPPConnection, JingleS5BTransportManager> INSTANCES = new WeakHashMap<>();

    private JingleS5BTransportManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleS5BTransportManager getInstanceFor(XMPPConnection connection) {
        JingleS5BTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleS5BTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Socks5BytestreamManager s5m = Socks5BytestreamManager.getBytestreamManager(connection());
        List<Jid> proxies = s5m.determineProxies();
        return determineStreamHostInfo(proxies);
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        return Socks5BytestreamManager.getBytestreamManager(connection())
                .getLocalStreamHost();
    }

    public List<Bytestream.StreamHost> determineStreamHostInfo(List<Jid> proxies) {
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
    protected JingleContentTransportProvider<JingleS5BTransport> createJingleContentTransportProvider() {
        return new JingleS5BTransportProvider();
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
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

    public JingleS5BTransport createJingleContentTransport(Jid remote, JingleContentTransport received_) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        JingleS5BTransport received = (JingleS5BTransport) received_;

        JingleS5BTransport.Builder builder = JingleS5BTransport.getBuilder();
        List<Bytestream.StreamHost> localStreams = getLocalStreamHosts();
        List<Bytestream.StreamHost> availableStreams = getAvailableStreamHosts();

        for (Bytestream.StreamHost host : localStreams) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 100);
            builder.addTransportCandidate(candidate);
        }

        for (Bytestream.StreamHost host : availableStreams) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 0);
            builder.addTransportCandidate(candidate);
        }

        String sid = (received == null ? JingleTransportManager.generateRandomId() : received.getStreamId());
        builder.setStreamId(sid);
        builder.setMode(received == null ? Bytestream.Mode.tcp : received.getMode());
        builder.setDestinationAddress(Socks5Utils.createDigest(sid, connection().getUser(), remote));
        return builder.build();
    }

    public static class Session {
        private ArrayList<Bytestream.StreamHost> ourStreamHosts;
        private ArrayList<Bytestream.StreamHost> theirStreamHosts;


    }

}
