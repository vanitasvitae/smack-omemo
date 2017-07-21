package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
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

    private JingleS5BTransport peersProposal;

    private JingleS5BTransportCandidate ourChoice, theirChoice;
    private JingleS5BTransportCandidate nominee;

    public JingleS5BTransport(String sid, String dstAddr, Bytestream.Mode mode, List<JingleS5BTransportCandidate> candidates) {
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

        if (theirChoice == null) {
            /*
            TODO: Booooooh illegal candidateId!! Go home!!!!11elf
             */
        }

        //connectIfReady();
    }

    private void handleCandidateActivate(JingleS5BTransportInfoElement info) {
        //Socks5BytestreamSession bs = new Socks5BytestreamSession(ourChoice.socket,
        //        ourChoice.candidate.getJid().asBareJid().equals(jingleSession.getRemote().asBareJid()));
        //callback.onSessionInitiated(bs);
    }

    private void handleCandidateError(JingleS5BTransportInfoElement info) {
        theirChoice = CANDIDATE_FAILURE;
        //connectIfReady();
    }

    private void handleProxyError(JingleS5BTransportInfoElement info) {
        //TODO
    }

    /**
     * Internal dummy candidate used to represent failure.
     * Kinda depressing, isn't it?
     */
    private final static JingleS5BTransportCandidate CANDIDATE_FAILURE = new JingleS5BTransportCandidate(null, null, -1, null);
}
