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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.jingle.AbstractJingleContentTransportManager;
import org.jivesoftware.smackx.jingle.JingleTransportInputStreamCallback;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleSocks5BytestreamTransportProvider;

/**
 * Manager for JingleSocks5BytestreamTransports.
 */
public final class JingleSocks5BytestreamTransportManager extends AbstractJingleContentTransportManager<JingleSocks5BytestreamTransport> {

    private JingleSocks5BytestreamTransportManager(XMPPConnection connection) {
        super(connection);
    }

    public ArrayList<Bytestream.StreamHost> getProxyIdentities() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        final ArrayList<Bytestream.StreamHost> streamHosts = new ArrayList<>();
        DiscoverInfo.Feature bytestreams = new DiscoverInfo.Feature(Bytestream.NAMESPACE);
        ServiceDiscoveryManager dm = ServiceDiscoveryManager.getInstanceFor(connection());
        DiscoverItems disco = dm.discoverItems(connection().getXMPPServiceDomain());

        for (DiscoverItems.Item item : disco.getItems()) {
            DiscoverInfo info = dm.discoverInfo(item.getEntityID());
            if (!info.getFeatures().contains(bytestreams)) {
                continue;
            }
            List<DiscoverInfo.Identity> identities = info.getIdentities("proxy", "bytestreams");
            if (identities.isEmpty()) {
                continue;
            }
            Bytestream b = new Bytestream();
            b.setTo(item.getEntityID());
            connection().sendIqWithResponseCallback(b, new StanzaListener() {
                @Override
                public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException {
                    Bytestream result = (Bytestream) packet;
                    if (result != null) {
                        streamHosts.addAll(result.getStreamHosts());
                    }
                }
            });
        }
        return streamHosts;
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
    public void acceptInputStream(Jingle jingle, JingleTransportInputStreamCallback callback) {

    }

    @Override
    public OutputStream createOutputStream(Jingle jingle) {
        return null;
    }
}
