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
import java.net.Socket;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * Implementation of a Jingle SOCKS5 Transport handler.
 */
public class JingleS5BTransportHandler implements JingleTransportHandler<JingleS5BTransport> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportHandler.class.getName());

    private JingleS5BTransportHandlerInterface state;
    private final JingleSessionHandler sessionHandler;
    private JingleTransportEstablishedCallback callback;

    private boolean localCandidateError = false;
    private boolean remoteCandidateError = false;
    private Socket connectedSocket;

    private JingleS5BTransportCandidate usedCandidate;

    public JingleS5BTransportHandler(JingleSessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    @Override
    public boolean onTransportInfoReceived(Jingle transportInfo) {
        boolean handled = state.onTransportInfoReceived(transportInfo);
        if (handled) {
            return true;
        }

        return false;
    }

    @Override
    public void prepareSession() {
        JingleS5BTransport proposedTransport = (JingleS5BTransport) sessionHandler.getProposedContent().getJingleTransports().get(0);
        Socks5Proxy.getSocks5Proxy().addTransfer(proposedTransport.getDestinationAddress());
    }

    @Override
    public void establishOutgoingSession(JingleTransportEstablishedCallback callback) {
        LOGGER.log(Level.INFO, "establish outgoing session");
        establishSession(callback);
    }

    @Override
    public void establishIncomingSession(JingleTransportEstablishedCallback callback) {
        establishSession(callback);
    }

    private void establishSession(JingleTransportEstablishedCallback callback) {
        this.callback = callback;
        JingleS5BTransport receivedTransport = (JingleS5BTransport) sessionHandler.getReceivedContent().getJingleTransports().get(0);

        JingleS5BTransportCandidate connectedCandidate = null;

        for (JingleContentTransportCandidate c : receivedTransport.getCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
            Bytestream.StreamHost streamHost = candidate.getStreamHost();
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            // establish socket
            try {
                final Socks5Client socks5Client = new Socks5Client(streamHost, receivedTransport.getDestinationAddress());
                connectedSocket = socks5Client.getSocket(10 * 1000);
                connectedCandidate = candidate;
                LOGGER.log(Level.INFO, "Connected to " + address + " using " + receivedTransport.getDestinationAddress());
                break;
            }
            catch (TimeoutException | IOException | SmackException | XMPPException | InterruptedException e) {
                //We cannot connect to transport candidate. Try others...
                LOGGER.log(Level.WARNING, "Could not connect to " + address + ": " + e, e);
            }
        }

        // Send candidate-used
        if (connectedCandidate != null) {
            LOGGER.log(Level.INFO, "Send candidate used");
            Jingle.Builder jb = Jingle.getBuilder();
            jb.setSessionId(sessionHandler.getFullJidAndSessionId().getSessionId())
                    .setAction(JingleAction.transport_info);

            JingleContent.Builder cb = JingleContent.getBuilder();
            cb.setSenders(sessionHandler.getProposedContent().getSenders())
                    .setCreator(sessionHandler.getProposedContent().getCreator())
                    .setName(sessionHandler.getProposedContent().getName());

            JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
            tb.setStreamId(((JingleS5BTransport) sessionHandler.getProposedContent().getJingleTransports().get(0)).getStreamId())
                    .setCandidateUsed(connectedCandidate.getCandidateId());

            Jingle jingle = jb.addJingleContent(cb.addTransport(tb.build()).build()).build();
            jingle.setTo(sessionHandler.getFullJidAndSessionId().getFullJid());
            jingle.setFrom(getConnection().getUser());

            this.usedCandidate = connectedCandidate;

            try {
                getConnection().sendStanza(jingle);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                callback.onSessionFailure(new JingleTransportFailureException(e));
            }
        } else {
            //Candidate error
            LOGGER.log(Level.INFO, "Send candidate-error");
            localCandidateError = true;
            Jingle.Builder jb = Jingle.getBuilder();
            jb.setAction(JingleAction.transport_info)
                    .setSessionId(sessionHandler.getFullJidAndSessionId().getSessionId());

            JingleContent.Builder cb = JingleContent.getBuilder();
            cb.setName(sessionHandler.getProposedContent().getName())
                    .setCreator(sessionHandler.getProposedContent().getCreator())
                    .setSenders(sessionHandler.getProposedContent().getSenders());

            JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
            tb.setCandidateError()
                    .setStreamId(((JingleS5BTransport) sessionHandler.getProposedContent().getJingleTransports().get(0))
                    .getStreamId());
            cb.addTransport(tb.build());

            jb.addJingleContent(cb.build());

            Jingle jingle = jb.build();
            jingle.setFrom(getConnection().getUser());
            jingle.setTo(sessionHandler.getFullJidAndSessionId().getFullJid());

            try {
                getConnection().sendStanza(jingle);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                callback.onSessionFailure(new JingleTransportFailureException(e));
            }
        }

        this.state = new FreshEstablishing(this);
    }

    @Override
    public XMPPConnection getConnection() {
        return sessionHandler.getConnection();
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
    }

    private interface JingleS5BTransportHandlerInterface {
        boolean onTransportInfoReceived(Jingle transportInfo);
    }

    private class FreshEstablishing implements JingleS5BTransportHandlerInterface{

        private final JingleS5BTransportHandler parent;

        public FreshEstablishing(JingleS5BTransportHandler parent) {
            this.parent = parent;
        }

        @Override
        public boolean onTransportInfoReceived(Jingle transportInfo) {
            if (transportInfo.getAction() != JingleAction.transport_info) {
                throw new IllegalArgumentException("Jingle Action must be transport-info.");
            }

            JingleContent content = transportInfo.getContents().get(0);
            JingleS5BTransport transport = (JingleS5BTransport) content.getJingleTransports().get(0);
            JingleS5BTransportInfo info = (JingleS5BTransportInfo) transport.getInfos().get(0);

            if (info == null) {
                throw new IllegalArgumentException("Jingle must contain at least one JingleS5BTransportInfo.");
            }

            switch (info.getElementName()) {
                case JingleS5BTransportInfo.CandidateUsed.ELEMENT:
                    JingleS5BTransportInfo.CandidateUsed candidateUsed = (JingleS5BTransportInfo.CandidateUsed) info;

                    //do we know the used candidate?
                    JingleS5BTransportCandidate usedCandidate = ((JingleS5BTransport) sessionHandler.getProposedContent().getJingleTransports().get(0))
                            .getCandidate(candidateUsed.getCandidateId());

                    if (usedCandidate == null) {
                        //Error unknown candidate.
                        //TODO: Ignore? Not specified in xep-0260
                    } else {

                        //We already have a remote candidate selected.
                        if (parent.usedCandidate == null ||
                                parent.usedCandidate.getPriority() < usedCandidate.getPriority() ||
                                (parent.usedCandidate.getPriority() == usedCandidate.getPriority() &&
                                parent.sessionHandler.getRole() == JingleContent.Creator.initiator)) {
                            parent.usedCandidate = usedCandidate;
                            Bytestream.StreamHost streamHost = usedCandidate.getStreamHost();
                            String address = streamHost.getAddress() + ":" + streamHost.getPort();
                            String digest;
                            if (parent.sessionHandler.getProposedContent().getJingleTransports().get(0).getCandidates().contains(parent.usedCandidate)) {
                                digest = ((JingleS5BTransport) parent.sessionHandler.getProposedContent().getJingleTransports().get(0)).getDestinationAddress();
                            } else {
                                digest = ((JingleS5BTransport) parent.sessionHandler.getReceivedContent().getJingleTransports().get(0)).getDestinationAddress();
                            }

                            // establish socket
                            try {
                                final Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest, getConnection(),
                                        parent.sessionHandler.getFullJidAndSessionId().getSessionId(),
                                        parent.sessionHandler.getFullJidAndSessionId().getFullJid());
                                connectedSocket = socks5Client.getSocket(10 * 1000);
                                LOGGER.log(Level.INFO, "Connected to " + address + " using " + digest);
                            }
                            catch (TimeoutException | IOException | SmackException | XMPPException | InterruptedException e) {
                                LOGGER.log(Level.WARNING, "Could not connect to own proxy at " + address + ": " + e, e);
                                callback.onSessionFailure(new JingleTransportFailureException(e));
                            }
                        }

                        parent.state = new CandidateUsedReceived(parent);
                    }
                    break;

                case JingleS5BTransportInfo.CandidateActivated.ELEMENT:
                    //Tie break
                    break;

                case JingleS5BTransportInfo.CandidateError.ELEMENT:
                    parent.remoteCandidateError = true;
                    if (parent.localCandidateError) {
                        //Session transport-failed
                        //TODO: Fallback
                    } else {
                        state = new CandidateUsedReceived(parent);
                    }
                    break;

                case JingleS5BTransportInfo.ProxyError.ELEMENT:
                    //???
                    break;
            }

            return false;
        }
    }

    private class CandidateUsedReceived implements JingleS5BTransportHandlerInterface {

        private final JingleS5BTransportHandler parent;

        public CandidateUsedReceived(JingleS5BTransportHandler parent) {
            super();
            this.parent = parent;

            activateTransports();
        }

        @Override
        public boolean onTransportInfoReceived(Jingle transportInfo) {
            JingleContent content = transportInfo.getContents().get(0);
            JingleS5BTransport transport = (JingleS5BTransport) content.getJingleTransports().get(0);
            JingleS5BTransportInfo info = (JingleS5BTransportInfo) transport.getInfos().get(0);

            if (JingleS5BTransportInfo.CandidateActivated.ELEMENT.equals(info.getElementName())) {
                JingleS5BTransportInfo.CandidateActivated candidateActivated =
                        (JingleS5BTransportInfo.CandidateActivated) info;

                if (!parent.usedCandidate.getCandidateId().equals(candidateActivated.getCandidateId())) {
                    //TODO: Unknown candidate. Not specified in xep-0260?
                } else {
                    LOGGER.log(Level.INFO, "Established connection with " + parent.usedCandidate.getHost());
                    callback.onSessionEstablished(new Socks5BytestreamSession(parent.connectedSocket, parent.usedCandidate.getType() == JingleS5BTransportCandidate.Type.direct));
                }
                return true;
            }
            return false;
        }

        void activateTransports() {
            //If proxy...
            if (parent.usedCandidate.getType() == JingleS5BTransportCandidate.Type.proxy) {
                JingleS5BTransport transport = (JingleS5BTransport) parent.sessionHandler
                        .getProposedContent().getJingleTransports().get(0);
                // ...and our candidate
                if (transport.getCandidates().contains(parent.usedCandidate)) {
                    if (!parent.usedCandidate.getJid().asFullJidIfPossible().equals(getConnection().getUser().asFullJidIfPossible())) {
                        // activate proxy.
                        Bytestream activateProxy = new Bytestream(transport.getStreamId());
                        activateProxy.setToActivate(parent.usedCandidate.getJid());
                        activateProxy.setTo(parent.usedCandidate.getJid());
                        try {
                            getConnection().createStanzaCollectorAndSend(activateProxy).nextResultOrThrow();
                        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
                            LOGGER.log(Level.SEVERE, "Could not activate proxy server: " + e, e);
                            callback.onSessionFailure(new JingleTransportFailureException(e));
                        }
                    }

                    //Send candidate-activate
                    Jingle.Builder jb = Jingle.getBuilder();
                    jb.setAction(JingleAction.transport_info)
                            .setSessionId(parent.sessionHandler.getFullJidAndSessionId().getSessionId())
                            .setInitiator(
                                    parent.sessionHandler.getRole() == JingleContent.Creator.initiator ?
                                            getConnection().getUser() : parent.sessionHandler.getFullJidAndSessionId().getFullJid());

                    JingleContent proposed = parent.sessionHandler.getProposedContent();

                    JingleContent.Builder cb = JingleContent.getBuilder();
                    cb.setName(proposed.getName())
                            .setCreator(proposed.getCreator())
                            .setSenders(proposed.getSenders());

                    JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
                    tb.setCandidateActivated(parent.usedCandidate.getCandidateId())
                            .setStreamId(((JingleS5BTransport) proposed.getJingleTransports().get(0)).getStreamId());

                    cb.addTransport(tb.build());
                    jb.addJingleContent(cb.build());

                    Jingle j = jb.build();
                    j.setTo(parent.sessionHandler.getFullJidAndSessionId().getFullJid());
                    j.setFrom(getConnection().getUser());
                    try {
                        getConnection().sendStanza(j);
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Could not send candidate-activated : " + e, e);
                        callback.onSessionFailure(new JingleTransportFailureException(e));
                    }

                    LOGGER.log(Level.INFO, "Established connection with " + parent.usedCandidate.getHost());
                    callback.onSessionEstablished(new Socks5BytestreamSession(parent.connectedSocket, parent.usedCandidate.getType() == JingleS5BTransportCandidate.Type.direct));
                }
            }
        }
    }
}
