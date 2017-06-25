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
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationException;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * LOL.
 */
public class JingleS5BTransportSession extends JingleTransportSession<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportSession.class.getName());
    private final JingleS5BTransportManager transportManager;

    private Socket connectedSocket;
    private JingleS5BTransportCandidate localUsedCandidate;
    private JingleS5BTransportCandidate remoteUsedCandidate;
    private JingleTransportInitiationCallback callback;
    private boolean remoteError = false;
    private boolean localError = false;

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
        JingleSession jSession = jingleSession.get();
        if (jSession == null) {
            throw new NullPointerException("Lost reference to JingleSession.");
        }

        JingleS5BTransport.Builder builder = JingleS5BTransport.getBuilder();

        for (Bytestream.StreamHost host : transportManager.getLocalStreamHosts()) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 100);
            builder.addTransportCandidate(candidate);
        }

        List<Bytestream.StreamHost> availableStreamHosts = null;

        try {
            availableStreamHosts = transportManager.getAvailableStreamHosts();
        } catch (XMPPException.XMPPErrorException | SmackException.NoResponseException | InterruptedException |
                SmackException.NotConnectedException e) {
            LOGGER.log(Level.WARNING, "Could not get available StreamHosts: " + e, e);
        }

        for (Bytestream.StreamHost host : availableStreamHosts != null ?
                availableStreamHosts : Collections.<Bytestream.StreamHost>emptyList()) {
            JingleS5BTransportCandidate candidate = new JingleS5BTransportCandidate(host, 0);
            builder.addTransportCandidate(candidate);
        }

        builder.setStreamId(sid);
        builder.setMode(mode);
        builder.setDestinationAddress(Socks5Utils.createDigest(sid, jSession.getLocal(), jSession.getRemote()));
        return builder.build();
    }

    @Override
    public void initiateOutgoingSession(JingleTransportInitiationCallback callback) {
        this.callback = callback;
        initiateSession();
    }

    @Override
    public void initiateIncomingSession(JingleTransportInitiationCallback callback) {
        this.callback = callback;
        initiateSession();
    }

    private void initiateSession() {
        JingleSession jSession = jingleSession.get();
        if (jSession == null) {
            throw new NullPointerException("Lost reference to jingleSession.");
        }

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

            JingleContent content = jSession.getContents().get(0);

            Jingle response;

            if (socket != null) {
                connectedSocket = socket;
                localUsedCandidate = workedForUs;

                response = transportManager.createCandidateUsed(jSession.getRemote(),
                        jSession.getSessionId(), content.getSenders(), content.getCreator(),
                        content.getName(), receivedTransport.getStreamId(), localUsedCandidate.getCandidateId());

            } else {
                localError = true;
                response = transportManager.createCandidateError(jSession.getRemote(),
                        jSession.getSessionId(), content.getSenders(), content.getCreator(),
                        content.getName(), receivedTransport.getStreamId());
            }

            try {
                jSession.getConnection().sendStanza(response);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-used.", e);
            }

            closeIfBothSidesFailed();
        }
    }

    private boolean closeIfBothSidesFailed() {
        JingleSession jSession = jingleSession.get();
        if (jSession != null) {
            if (localError && remoteError) {
                callback.onException(new JingleTransportInitiationException.CandidateError());
                return true;
            }
        }
    }

    private JingleS5BTransportCandidate determineUsedCandidate() {
        if (localUsedCandidate == null && remoteUsedCandidate == null) {
            return null;
        }

        if (remoteUsedCandidate == null) {
            return localUsedCandidate;
        }

        if (localUsedCandidate == null) {
            return remoteUsedCandidate;
        }

        if (localUsedCandidate.getPriority() > remoteUsedCandidate.getPriority()) {
            return localUsedCandidate;
        }

        if (localUsedCandidate.getPriority() < remoteUsedCandidate.getPriority()) {
            return remoteUsedCandidate;
        }

        return jingleSession.get().isInitiator() ? localUsedCandidate : remoteUsedCandidate;
    }

    public IQ handleCandidateUsed(Jingle candidateUsed) {
        JingleS5BTransportInfo info = (JingleS5BTransportInfo) candidateUsed.getContents().get(0)
                .getJingleTransport().getInfos().get(0);

        String candidateId = ((JingleS5BTransportInfo.CandidateUsed) info).getCandidateId();

        for (JingleContentTransportCandidate c : localTransport.getCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
            if (candidate.getCandidateId().equals(candidateId)) {
                remoteUsedCandidate = candidate;
                break;
            }
        }

        if (remoteUsedCandidate == null) {
            callback.onException(new Exception("Unknown candidate"));
            return IQ.createResultIQ(candidateUsed);
        }

        if (localUsedCandidate != null) {
            JingleS5BTransportCandidate used = determineUsedCandidate();

            // Our candidate is nominated.
            if (used == remoteUsedCandidate) {

                if (used.getType() == JingleS5BTransportCandidate.Type.proxy) {

                }



                callback.onSessionInitiated();
            }
            // Remotes candidate is nominated.
            else {
                if (connectedSocket != null) {
                    callback.onSessionInitiated(new Socks5BytestreamSession(connectedSocket,
                            used.getJid().asBareJid().equals(jingleSession.get().getRemote().asBareJid())));
                } else {
                    throw new AssertionError("Connected socket is null.");
                }
            }
        }

        return IQ.createResultIQ(candidateUsed);
    }

    @Override
    public String getNamespace() {
        return transportManager.getNamespace();
    }

    @Override
    public IQ handleTransportInfo(Jingle transportInfo) {
        JingleS5BTransport transport = (JingleS5BTransport) transportInfo.getContents().get(0).getJingleTransport();
        JingleS5BTransportInfo info = (JingleS5BTransportInfo) transport.getInfos().get(0);
        if (info != null) {
            switch (info.getElementName()) {

                case JingleS5BTransportInfo.CandidateUsed.ELEMENT:
                    return handleCandidateUsed(transportInfo);

                case JingleS5BTransportInfo.CandidateActivated.ELEMENT:

                case JingleS5BTransportInfo.CandidateError.ELEMENT:
                    remoteError = true;

                    closeIfBothSidesFailed();

                    if (localUsedCandidate.getType() != JingleS5BTransportCandidate.Type.proxy) {
                        //TODO: Connect
                    } else {

                    }

                    break;

                case JingleS5BTransportInfo.ProxyError.ELEMENT:

            }
        }
    }

    @Override
    public JingleTransportManager<JingleS5BTransport> transportManager() {
        return JingleS5BTransportManager.getInstanceFor(jingleSession.get().getConnection());
    }

}
