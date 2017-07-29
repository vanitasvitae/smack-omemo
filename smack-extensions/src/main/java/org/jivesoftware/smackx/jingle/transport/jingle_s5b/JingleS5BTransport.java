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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.components.JingleContent;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.components.JingleTransport;
import org.jivesoftware.smackx.jingle.components.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.exception.FailedTransportException;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;

import org.jxmpp.jid.FullJid;

/**
 * Jingle SOCKS5Bytestream transport component.
 */
public class JingleS5BTransport extends JingleTransport<JingleS5BTransportElement> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransport.class.getName());

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:s5b:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    public static final int MAX_TIMEOUT = 10 * 1000;

    private final String sid;

    private String dstAddr;
    private Bytestream.Mode mode;

    // PEERS candidate of OUR choice.
    private JingleS5BTransportCandidate selectedCandidate;

    private JingleTransportCallback callback;

    /**
     * Create fresh JingleS5BTransport.
     * @param initiator initiator.
     * @param responder responder.
     */
    public JingleS5BTransport(FullJid initiator, FullJid responder, String sid, List<JingleTransportCandidate<?>> candidates) {
        this(sid, Socks5Utils.createDigest(sid, initiator, responder), Bytestream.Mode.tcp, candidates);
    }

    public JingleS5BTransport(JingleContent content, JingleS5BTransport other, List<JingleTransportCandidate<?>> candidates) {
        this(other.getSid(),
                Socks5Utils.createDigest(other.getSid(), content.getParent().getInitiator(), content.getParent().getResponder()),
                other.mode, candidates);
    }

    public JingleS5BTransport(String sid, String dstAddr, Bytestream.Mode mode, List<JingleTransportCandidate<?>> candidates) {
        this.sid = sid;
        this.dstAddr = dstAddr;
        this.mode = mode;

        for (JingleTransportCandidate<?> c : (candidates != null ?
                candidates : Collections.<JingleS5BTransportCandidate>emptySet())) {
            addCandidate(c);
        }
    }

    @Override
    public JingleS5BTransportElement getElement() {
        JingleS5BTransportElement.Builder builder = JingleS5BTransportElement.getBuilder()
                .setStreamId(sid)
                .setDestinationAddress(dstAddr)
                .setMode(mode);

        for (JingleTransportCandidate<?> candidate : getCandidates()) {
            builder.addTransportCandidate((JingleS5BTransportCandidateElement) candidate.getElement());
        }

        return builder.build();
    }

    public String getSid() {
        return sid;
    }

    public String getDstAddr() {
        return dstAddr;
    }

    public Bytestream.Mode getMode() {
        return mode;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException {
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    @Override
    public void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException {
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    void establishBytestreamSession(XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException {
        Socks5Proxy.getSocks5Proxy().addTransfer(dstAddr);
        JingleS5BTransportManager transportManager = JingleS5BTransportManager.getInstanceFor(connection);
        this.selectedCandidate = connectToCandidates(MAX_TIMEOUT);

        if (selectedCandidate == CANDIDATE_FAILURE) {
            connection.createStanzaCollectorAndSend(transportManager.createCandidateError(this));
            return;
        }

        if (selectedCandidate == null) {
            throw new AssertionError("MUST NOT BE NULL.");
        }

        connection.createStanzaCollectorAndSend(transportManager.createCandidateUsed(this, selectedCandidate));
        connectIfReady();
    }

    public JingleS5BTransportCandidate connectToCandidates(int timeout) {
        for (JingleTransportCandidate<?> c : getCandidates()) {
            int _timeout = timeout / getCandidates().size(); //TODO: Wise?
            try {
                return ((JingleS5BTransportCandidate) c).connect(_timeout);
            } catch (IOException | TimeoutException | InterruptedException | SmackException | XMPPException e) {
                LOGGER.log(Level.WARNING, "Exception while connecting to candidate: " + e, e);
            }
        }

        // Failed to connect to any candidate.
        return CANDIDATE_FAILURE;
    }

    void connectIfReady() {
        JingleS5BTransportManager jingleS5BTransportManager = JingleS5BTransportManager.getInstanceFor(getParent().getParent().getJingleManager().getConnection());
        JingleS5BTransport peers = (JingleS5BTransport) getPeersProposal();
        JingleSession session = getParent().getParent();

        if (getSelectedCandidate() == null || peers.getSelectedCandidate() == null) {
            // Not yet ready if we or peer did not yet decide on a candidate.
            LOGGER.log(Level.INFO, "Not ready.");
            return;
        }

        if (getSelectedCandidate() == CANDIDATE_FAILURE && peers.getSelectedCandidate() == CANDIDATE_FAILURE) {
            LOGGER.log(Level.INFO, "Failure.");
            callback.onTransportFailed(new FailedTransportException(null));
            return;
        }

        LOGGER.log(Level.INFO, "Ready.");

        //Determine nominated candidate.
        JingleS5BTransportCandidate nominated;
        if (getSelectedCandidate() != CANDIDATE_FAILURE && peers.getSelectedCandidate() != CANDIDATE_FAILURE) {

            if (getSelectedCandidate().getPriority() > peers.getSelectedCandidate().getPriority()) {
                nominated = getSelectedCandidate();
            } else if (getSelectedCandidate().getPriority() < peers.getSelectedCandidate().getPriority()) {
                nominated = peers.getSelectedCandidate();
            } else {
                nominated = getParent().getParent().isInitiator() ? getSelectedCandidate() : peers.getSelectedCandidate();
            }

        } else if (getSelectedCandidate() != CANDIDATE_FAILURE) {
            nominated = getSelectedCandidate();
        } else {
            nominated = peers.getSelectedCandidate();
        }

        if (nominated == peers.getSelectedCandidate()) {

            LOGGER.log(Level.INFO, "Their choice, so our proposed candidate is used.");
            boolean isProxy = nominated.getType() == JingleS5BTransportCandidateElement.Type.proxy;

            try {
                nominated = nominated.connect(MAX_TIMEOUT);
            } catch (InterruptedException | IOException | XMPPException | SmackException | TimeoutException e) {
                LOGGER.log(Level.INFO, "Could not connect to our candidate.", e);
                callback.onTransportFailed(new S5BTransportException.CandidateError(e));
                return;
            }

            if (isProxy) {

                LOGGER.log(Level.INFO, "Is external proxy. Activate it.");
                Bytestream activate = new Bytestream(getSid());
                activate.setMode(null);
                activate.setType(IQ.Type.set);
                activate.setTo(nominated.getStreamHost().getJID());
                activate.setToActivate(getParent().getParent().getPeer());
                activate.setFrom(getParent().getParent().getOurJid());

                try {
                    getParent().getParent().getJingleManager().getConnection().createStanzaCollectorAndSend(activate).nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not activate proxy.", e);
                    callback.onTransportFailed(new S5BTransportException.ProxyError(e));
                    return;
                }

                LOGGER.log(Level.INFO, "Send candidate-activate.");
                JingleElement candidateActivate = jingleS5BTransportManager.createCandidateActivated((JingleS5BTransport) nominated.getParent(), nominated);

                try {
                    session.getJingleManager().getConnection().createStanzaCollectorAndSend(candidateActivate)
                            .nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not send candidate-activated", e);
                    callback.onTransportFailed(new S5BTransportException.ProxyError(e));
                    return;
                }
            }

            LOGGER.log(Level.INFO, "Start transmission.");
            this.bytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), !isProxy);
            callback.onTransportReady(this.bytestreamSession);

        }
        //Our choice
        else {
            LOGGER.log(Level.INFO, "Our choice, so their candidate was used.");
            boolean isProxy = nominated.getType() == JingleS5BTransportCandidateElement.Type.proxy;
            if (!isProxy) {
                LOGGER.log(Level.INFO, "Direct connection.");
                this.bytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), true);
                callback.onTransportReady(this.bytestreamSession);
            } else {
                LOGGER.log(Level.INFO, "Our choice was their external proxy. wait for candidate-activate.");
            }
        }
    }

    @Override
    public IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping) {
        switch (info.getElementName()) {

            case JingleS5BTransportInfoElement.CandidateUsed.ELEMENT:
                handleCandidateUsed((JingleS5BTransportInfoElement) info, wrapping);
                break;

            case JingleS5BTransportInfoElement.CandidateActivated.ELEMENT:
                handleCandidateActivate((JingleS5BTransportInfoElement) info);
                break;

            case JingleS5BTransportInfoElement.CandidateError.ELEMENT:
                handleCandidateError((JingleS5BTransportInfoElement) info);
                break;

            case JingleS5BTransportInfoElement.ProxyError.ELEMENT:
                handleProxyError((JingleS5BTransportInfoElement) info);
                break;

            default:
                throw new AssertionError("Unknown transport-info element: " + info.getElementName());
        }

        return IQ.createResultIQ(wrapping);
    }

    private void handleCandidateUsed(JingleS5BTransportInfoElement info, JingleElement wrapping) {
        JingleManager jingleManager = getParent().getParent().getJingleManager();
        String candidateId = ((JingleS5BTransportInfoElement.CandidateUsed) info).getCandidateId();

        JingleS5BTransport peers = (JingleS5BTransport) getPeersProposal();

        // Received second candidate-used -> out-of-order!
        if (peers.getSelectedCandidate() != null) {
            try {
                jingleManager.getConnection().createStanzaCollectorAndSend(JingleElement.createJingleErrorOutOfOrder(wrapping));
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Could not respond to candidate-used transport-info: " + e, e);
            }
            return;
        }

        Iterator<JingleTransportCandidate<?>> ourCandidates = getCandidates().iterator();
        while (ourCandidates.hasNext()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) ourCandidates.next();
            if (candidate.getCandidateId().equals(candidateId)) {
                peers.setSelectedCandidate(candidate);
            }
        }

        if (peers.getSelectedCandidate() == null) {
            //TODO: Alert! Illegal candidateId!
        }

        connectIfReady();
    }

    private void handleCandidateActivate(JingleS5BTransportInfoElement info) {
        this.bytestreamSession = new Socks5BytestreamSession(getSelectedCandidate().getSocket(),
                getSelectedCandidate().getStreamHost().getJID().asBareJid().equals(getParent().getParent().getPeer().asBareJid()));
        callback.onTransportReady(this.bytestreamSession);
    }

    private void handleCandidateError(JingleS5BTransportInfoElement info) {
        ((JingleS5BTransport) getPeersProposal()).setSelectedCandidate(CANDIDATE_FAILURE);
        connectIfReady();
    }

    private void handleProxyError(JingleS5BTransportInfoElement info) {
        callback.onTransportFailed(new S5BTransportException.ProxyError(null));
    }

    public void setSelectedCandidate(JingleS5BTransportCandidate candidate) {
        selectedCandidate = candidate;
    }

    public JingleS5BTransportCandidate getSelectedCandidate() {
        return selectedCandidate;
    }

    /**
     * Internal dummy candidate used to represent failure.
     * Kinda depressing, isn't it?
     */
    private final static JingleS5BTransportCandidate CANDIDATE_FAILURE = new JingleS5BTransportCandidate(null, null, -1, null);

    @Override
    protected boolean isNonFatalException(Exception exception) {
        return false;
    }

    @Override
    protected void handleStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException {

    }
}
