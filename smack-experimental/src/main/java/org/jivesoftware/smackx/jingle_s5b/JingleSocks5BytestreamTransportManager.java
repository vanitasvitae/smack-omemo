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
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.jingle.AbstractJingleContentTransportManager;
import org.jivesoftware.smackx.jingle.JingleTransportInputStreamCallback;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleSocks5BytestreamTransportProvider;
import org.jxmpp.jid.Jid;

/**
 * Manager for JingleSocks5BytestreamTransports.
 */
public final class JingleSocks5BytestreamTransportManager extends AbstractJingleContentTransportManager<JingleSocks5BytestreamTransport> {

    private JingleSocks5BytestreamTransportManager(XMPPConnection connection) {
        super(connection);
    }

    public List<Bytestream.StreamHost> getAvailableStreamHosts() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        Socks5BytestreamManager s5m = Socks5BytestreamManager.getBytestreamManager(connection());
        List<Jid> proxies = s5m.determineProxies();
        return s5m.determineStreamHostInfos(proxies);
    }

    public List<Bytestream.StreamHost> getLocalStreamHosts() {
        return Socks5BytestreamManager.getBytestreamManager(connection())
                .getLocalStreamHost();
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

    @Override
    public JingleSocks5BytestreamTransport createJingleContentTransport(Jid otherUser) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
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

        builder.setMode(Bytestream.Mode.tcp);
        String sid = JingleTransportManager.generateRandomId();
        builder.setStreamId(sid);
        String digestString =
                        sid +
                        connection().getUser().asFullJidIfPossible().toString() +
                        otherUser.asFullJidIfPossible().toString();
        try {
            builder.setDestinationAddress(
                    new String(HashManager.sha_1(digestString.getBytes(StringUtils.UTF8)), StringUtils.UTF8));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        return builder.build();
    }
}
