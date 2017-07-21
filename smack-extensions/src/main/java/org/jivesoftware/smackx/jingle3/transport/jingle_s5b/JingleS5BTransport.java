package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle3.internal.Content;
import org.jivesoftware.smackx.jingle3.internal.Transport;
import org.jivesoftware.smackx.jingle3.internal.TransportCandidate;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportInfoElement;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 19.07.17.
 */
public class JingleS5BTransport extends Transport<JingleS5BTransportElement> {

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:s5b:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    private final String sid;

    private String dstAddr;
    private Bytestream.Mode mode;

    // PEERS candidate of OUR choice.
    private JingleS5BTransportCandidate selectedCandidate;

    /**
     * Create fresh JingleS5BTransport.
     * @param content parent content.
     */
    public JingleS5BTransport(Content content) {
        super(content);
        this.sid = StringUtils.randomString(24);
        this.dstAddr = Socks5Utils.createDigest(sid, content.getParent().getInitiator(), content.getParent().getResponder());
        this.mode = Bytestream.Mode.tcp;
    }

    public JingleS5BTransport(Content content, String sid, String dstAddr, Bytestream.Mode mode, List<JingleS5BTransportCandidate> candidates) {
        super(content);
        this.sid = sid;
        this.dstAddr = dstAddr;
        this.mode = mode;

        for (TransportCandidate<JingleS5BTransportCandidateElement> c : (candidates != null ?
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

        for (TransportCandidate candidate : getCandidates()) {
            builder.addTransportCandidate((JingleS5BTransportCandidateElement) candidate.getElement());
        }

        return builder.build();
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
    public void establishIncomingBytestreamSession(FullJid peer, String transportSessionId, BytestreamSessionEstablishedListener listener, XMPPConnection connection) {
        Socks5Proxy.getSocks5Proxy().addTransfer(dstAddr);
    }

    @Override
    public void establishOutgoingBytestreamSession(FullJid peer, String transportSessionId, BytestreamSessionEstablishedListener listener, XMPPConnection connection) {

    }

    public JingleS5BTransportCandidate connectToCandidates(int timeout) {
        for (TransportCandidate c : getCandidates()) {
            int _timeout = timeout / getCandidates().size(); //TODO: Wise?
            try {
                return ((JingleS5BTransportCandidate) c).connect(_timeout);
            } catch (IOException | TimeoutException | InterruptedException | SmackException | XMPPException e) {
                e.printStackTrace();
            }
        }

        // Failed to connect to any candidate.
        return null;
    }

    @Override
    public void handleTransportInfo(JingleContentTransportInfoElement info) {
        switch (info.getElementName()) {

            case JingleS5BTransportInfoElement.CandidateUsed.ELEMENT:
                handleCandidateUsed((JingleS5BTransportInfoElement) info);
                return;

            case JingleS5BTransportInfoElement.CandidateActivated.ELEMENT:
                handleCandidateActivate((JingleS5BTransportInfoElement) info);
                return;

            case JingleS5BTransportInfoElement.CandidateError.ELEMENT:
                handleCandidateError((JingleS5BTransportInfoElement) info);
                return;

            case JingleS5BTransportInfoElement.ProxyError.ELEMENT:
                handleProxyError((JingleS5BTransportInfoElement) info);
                return;

            default:
                throw new AssertionError("Unknown transport-info element: " + info.getElementName());
        }
    }

    private void handleCandidateUsed(JingleS5BTransportInfoElement info) {
        String candidateId = ((JingleS5BTransportInfoElement.CandidateUsed) info).getCandidateId();

        JingleS5BTransport peers = (JingleS5BTransport) getPeersProposal();

        if (peers.getSelectedCandidate() != null) {
            //TODO: Alert! We already received one candidateUsed previously!
            return;
        }

        Iterator<TransportCandidate<?>> ourCandidates = getCandidates().iterator();

        while (ourCandidates.hasNext()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) ourCandidates.next();
            if (candidate.getCandidateId().equals(candidateId)) {
                peers.setSelectedCandidate(candidate);
            }
        }

        if (peers.getSelectedCandidate() == null) {
            //TODO: Alert! Illegal candidateId!
        }

        //connectIfReady();
    }

    private void handleCandidateActivate(JingleS5BTransportInfoElement info) {
        //Socks5BytestreamSession bs = new Socks5BytestreamSession(ourChoice.socket,
        //        ourChoice.candidate.getJid().asBareJid().equals(jingleSession.getRemote().asBareJid()));
        //callback.onSessionInitiated(bs);
    }

    private void handleCandidateError(JingleS5BTransportInfoElement info) {
        ((JingleS5BTransport) getPeersProposal()).setSelectedCandidate(CANDIDATE_FAILURE);
        //connectIfReady();
    }

    private void handleProxyError(JingleS5BTransportInfoElement info) {
        //TODO
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
}
