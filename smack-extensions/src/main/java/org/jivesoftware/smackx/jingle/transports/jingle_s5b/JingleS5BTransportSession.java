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

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;

/**
 * LOL.
 */
public class JingleS5BTransportSession extends JingleTransportSession<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportSession.class.getName());
    private final JingleS5BTransportManager transportManager;

    public JingleS5BTransportSession(JingleSession jingleSession) {
        super(jingleSession);
        transportManager = JingleS5BTransportManager.getInstanceFor(jingleSession.getConnection());
    }

    @Override
    public JingleS5BTransport createTransport() {
        if (localTransport != null) {
            return (JingleS5BTransport) localTransport;
        }

        return createTransport(JingleManager.randomId(), Bytestream.Mode.tcp);
    }

    private JingleS5BTransport createTransport(String sid, Bytestream.Mode mode) {
        JingleS5BTransport.Builder builder = JingleS5BTransport.getBuilder();

        for (Bytestream.StreamHost host : transportManager.getLocalStreamHosts()) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 100);
            builder.addTransportCandidate(candidate);
        }

        List<Bytestream.StreamHost> availableStreamHosts = null;

        try {
            availableStreamHosts = transportManager.getAvailableStreamHosts();
        } catch (XMPPException.XMPPErrorException | SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException e) {
            LOGGER.log(Level.WARNING, "Could not get available StreamHosts: " + e, e);
        }

        for (Bytestream.StreamHost host : availableStreamHosts != null ?
                availableStreamHosts : Collections.<Bytestream.StreamHost>emptyList()) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 0);
            builder.addTransportCandidate(candidate);
        }

        builder.setStreamId(sid);
        builder.setMode(mode);
        builder.setDestinationAddress(Socks5Utils.createDigest(sid, jingleSession.get().getLocal(), jingleSession.get().getRemote()));
        return builder.build();
    }

    @Override
    public void initiateOutgoingSession(JingleTransportInitiationCallback callback) {
        JingleS5BTransport receivedTransport = (JingleS5BTransport) remoteTransport;

        Socket socket = null;
        JingleS5BTransportCandidate workedForUs = null;

        for (JingleContentTransportCandidate c : receivedTransport.getCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
            Bytestream.StreamHost streamHost = candidate.getStreamHost();

            String address = streamHost.getAddress();

            try {
                Socks5Client socks5Client = new Socks5Client(streamHost, receivedTransport.getDestinationAddress());
                socket = socks5Client.getSocket(10 * 1000);
                workedForUs = candidate;

            } catch (IOException | XMPPException | InterruptedException | TimeoutException | SmackException e) {
                LOGGER.log(Level.WARNING, "Could not connect to remotes address " + address + " with dstAddr "
                        + receivedTransport.getDestinationAddress());
            }

            if (socket != null) {

            }
        }
    }

    @Override
    public void initiateIncomingSession(JingleTransportInitiationCallback callback) {

    }

    @Override
    public String getNamespace() {
        return transportManager.getNamespace();
    }

    @Override
    public IQ handleTransportInfo(Jingle transportInfo) {
        return null;
    }

    @Override
    public JingleTransportManager<JingleS5BTransport> transportManager() {
        return JingleS5BTransportManager.getInstanceFor(jingleSession.get().getConnection());
    }

}
