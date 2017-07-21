package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle3.internal.Session;
import org.jivesoftware.smackx.jingle3.internal.TransportCandidate;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportCandidateElement;

/**
 * Created by vanitas on 19.07.17.
 */
public class JingleS5BTransportCandidate extends TransportCandidate<JingleS5BTransportCandidateElement> {

    private final String candidateId;
    private final Bytestream.StreamHost streamHost;
    private final JingleS5BTransportCandidateElement.Type type;

    private Socket socket;

    public JingleS5BTransportCandidate(String candidateId,
                                       Bytestream.StreamHost streamHost,
                                       int priority,
                                       JingleS5BTransportCandidateElement.Type type) {
        this.candidateId = candidateId;
        this.streamHost = streamHost;
        this.type = type;

        setPriority(priority);
    }

    public static JingleS5BTransportCandidate fromElement(JingleS5BTransportCandidateElement element) {
        return new JingleS5BTransportCandidate(element.getCandidateId(), element.getStreamHost(), element.getPriority(), element.getType());
    }

    public String getCandidateId() {
        return candidateId;
    }

    public Bytestream.StreamHost getStreamHost() {
        return streamHost;
    }

    public JingleS5BTransportCandidateElement.Type getType() {
        return type;
    }

    public JingleS5BTransportCandidateElement getElement() {
        return new JingleS5BTransportCandidateElement(
                getCandidateId(), getStreamHost().getAddress(),
                getStreamHost().getJID(), getStreamHost().getPort(),
                getPriority(), getType());
    }

    public JingleS5BTransportCandidate connect(int timeout) throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        Socks5Client client;

        if (getParent().isPeersProposal()) {
            client = new Socks5Client(getStreamHost(), ((JingleS5BTransport) getParent()).getDstAddr());
        }
        else {
            Session session = getParent().getParent().getParent();
            client = new Socks5ClientForInitiator(getStreamHost(), ((JingleS5BTransport) getParent()).getDstAddr(),
                    session.getJingleManager().getConnection(), session.getSessionId(), session.getPeer());
        }

        this.socket = client.getSocket(timeout);
        return this;
    }

    public Socket getSocket() {
        return socket;
    }
}
