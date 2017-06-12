/**
 *
 * Copyright © 2017 Paul Schaub
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
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * JingleTransportHandler for Socks5Bytestreams.
 */
public class JingleS5BTransportHandler implements JingleTransportHandler<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportHandler.class.getName());

    private final WeakReference<JingleSessionHandler> sessionHandler;

    private JingleManager.FullJidAndSessionId fullJidAndSessionId;
    private JingleContent receivedContent;
    private JingleContent proposedContent;
    private JingleTransportEstablishedCallback callback;
    private JingleS5BTransport myTransport;

    private JingleS5BTransportCandidate receivedCandidateUsed = null;
    private JingleS5BTransportCandidate selectedCandidateUsed = null;

    private Socket connectedSocket = null;

    public JingleS5BTransportHandler(JingleSessionHandler sessionHandler) {
        this.sessionHandler = new WeakReference<>(sessionHandler);
    }

    @Override
    public void prepareOutgoingSession(JingleManager.FullJidAndSessionId fullJidAndSessionId, JingleContent content) {
        myTransport = (JingleS5BTransport) content.getJingleTransports().get(0);
        Socks5Proxy.getSocks5Proxy().addLocalAddress(myTransport.getDestinationAddress());
    }

    @Override
    public void establishOutgoingSession(JingleManager.FullJidAndSessionId fullJidAndSessionId,
                                         JingleContent receivedContent,
                                         JingleContent proposedContent,
                                         JingleTransportEstablishedCallback callback) {
        establishSession(fullJidAndSessionId, receivedContent, proposedContent, callback);
    }

    @Override
    public void establishIncomingSession(JingleManager.FullJidAndSessionId fullJidAndSessionId,
                                         JingleContent receivedContent,
                                         JingleContent proposedContent,
                                         JingleTransportEstablishedCallback callback) {
        establishSession(fullJidAndSessionId, receivedContent, proposedContent, callback);
    }

    void establishSession(JingleManager.FullJidAndSessionId fullJidAndSessionId,
                          JingleContent receivedContent,
                          JingleContent proposedContent,
                          JingleTransportEstablishedCallback callback) {
        this.fullJidAndSessionId = fullJidAndSessionId;
        this.receivedContent = receivedContent;
        this.proposedContent = proposedContent;
        this.callback = callback;

        JingleContentTransport hopefullyS5BTransport = receivedContent.getJingleTransports().get(0);
        if (!hopefullyS5BTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Transport must be a JingleS5BTransport.");
        }

        JingleS5BTransport transport = (JingleS5BTransport) hopefullyS5BTransport;

        Socks5Proxy.getSocks5Proxy().addLocalAddress(Socks5Utils.createDigest(
                fullJidAndSessionId.getSessionId(),                 //SessionID
                getConnection().getUser().asFullJidIfPossible(),    //Us
                fullJidAndSessionId.getFullJid()));                 //Them

        JingleS5BTransportCandidate usedCandidate = null;
        for (JingleContentTransportCandidate c : transport.getCandidates()) {
            usedCandidate = (JingleS5BTransportCandidate) c;
            Bytestream.StreamHost streamHost = usedCandidate.getStreamHost();
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            LOGGER.log(Level.INFO, "Connect to " + address);
            // establish socket
            try {
                final Socks5Client socks5Client = new Socks5Client(streamHost, transport.getDestinationAddress());
                connectedSocket = socks5Client.getSocket(10 * 1000);
                selectedCandidateUsed = usedCandidate;
                break;
            }
            catch (TimeoutException | IOException | SmackException | XMPPException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not connect to " + address + ": " + e, e);
            }
        }

        if (connectedSocket != null) {
            //Send candidate-used
            Jingle jingle = createCandidateUsed(usedCandidate, fullJidAndSessionId, receivedContent);
            try {
                getConnection().sendStanza(jingle);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-used stanza: " + e, e);
            }
        } else {

        }
    }

    void activateProxy() {
        //Activate proxy
        if (selectedCandidateUsed.getType() == JingleS5BTransportCandidate.Type.proxy) {
            Bytestream bytestream = new Bytestream(fullJidAndSessionId.getSessionId());
            bytestream.setToActivate(fullJidAndSessionId.getFullJid());
            bytestream.setTo(fullJidAndSessionId.getFullJid());
            try {
                getConnection().sendStanza(bytestream);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not activate proxy: " + e, e);
            }
        }

        Jingle activate = createCandidateActive(selectedCandidateUsed, fullJidAndSessionId, receivedContent);
        try {
            getConnection().sendStanza(activate);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Could not send transport-activated: " + e, e);
        }

        callback.onSessionEstablished(new Socks5BytestreamSession(connectedSocket, false));
    }

    Jingle createCandidateUsed(JingleS5BTransportCandidate usedCandidate, JingleManager.FullJidAndSessionId fullJidAndSessionId, JingleContent receivedContent) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(fullJidAndSessionId.getSessionId())
                .setAction(JingleAction.transport_info)
                .setInitiator(getConnection().getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(receivedContent.getName())
                .setCreator(receivedContent.getCreator())
                .setSenders(receivedContent.getSenders());

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setStreamId(((JingleS5BTransport) receivedContent.getJingleTransports().get(0)).getStreamId());
        tb.setCandidateUsed(usedCandidate.getCandidateId());
        cb.addTransport(tb.build());

        jb.addJingleContent(cb.build());

        Jingle jingle = jb.build();
        jingle.setFrom(getConnection().getUser());
        jingle.setTo(fullJidAndSessionId.getFullJid());
        return jingle;
    }

    Jingle createCandidateActive(JingleS5BTransportCandidate usedCandidate, JingleManager.FullJidAndSessionId fullJidAndSessionId, JingleContent receivedContent) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.transport_info)
                .setSessionId(fullJidAndSessionId.getSessionId())
                .setInitiator(fullJidAndSessionId.getFullJid());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(JingleContent.Creator.initiator)
                .setName(receivedContent.getName());

        JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
        tb.setStreamId(((JingleS5BTransport) receivedContent.getJingleTransports().get(0)).getStreamId());
        tb.setCandidateActivated(usedCandidate.getCandidateId());

        cb.addTransport(tb.build());
        jb.addJingleContent(cb.build());

        Jingle activate = jb.build();
        activate.setTo(fullJidAndSessionId.getFullJid());
        activate.setFrom(getConnection().getUser());

        return activate;
    }

    @Override
    public XMPPConnection getConnection() {
        JingleSessionHandler handler = sessionHandler.get();
        return handler != null ? handler.getConnection() : null;
    }

    @Override
    public void onTransportInfoReceived(Jingle jingle) {
        if (jingle.getAction() != JingleAction.transport_info) {
            throw new IllegalArgumentException("Jingle Action must be transport-info.");
        }
        JingleContentTransport jingleTransport = jingle.getContents().get(0).getJingleTransports().get(0);
        if (jingleTransport == null || !jingleTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Jingle must contain transport of type S5B.");
        }

        JingleS5BTransport transport = (JingleS5BTransport) jingleTransport;
        JingleS5BTransportInfo info = (JingleS5BTransportInfo) transport.getInfos().get(0);
        if (info == null) {
            throw new IllegalArgumentException("Jingle must contain at least one JingleS5BTransportInfo.");
        }

        JingleS5BTransportCandidate finalCandidateUsed;

        if (info.getElementName().equals(JingleS5BTransportInfo.CandidateUsed.ELEMENT)) {
            String candidateUsedId = ((JingleS5BTransportInfo.CandidateUsed) info).getCandidateId();
            for (JingleContentTransportCandidate c : myTransport.getCandidates()) {
                if (((JingleS5BTransportCandidate) c).getCandidateId().equals(candidateUsedId)) {
                    receivedCandidateUsed = (JingleS5BTransportCandidate) c;
                }
            }

            if (receivedCandidateUsed == null) {
                //TODO: Unknown candidate.
            } else {

            }
        }
    }

    void onCandidateUsed() {
        if (selectedCandidateUsed.getPriority() > receivedCandidateUsed.getPriority()) {
            activateProxy();
        } else if (selectedCandidateUsed.getPriority() < receivedCandidateUsed.getPriority()) {
            //createCandidateActive()
        } else { //==
        }
    }
}
