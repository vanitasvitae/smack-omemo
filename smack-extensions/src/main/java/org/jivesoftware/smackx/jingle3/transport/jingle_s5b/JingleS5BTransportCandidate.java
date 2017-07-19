package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportCandidateElement;

/**
 * Created by vanitas on 19.07.17.
 */
public class JingleS5BTransportCandidate {

    private final String candidateId;
    private final Bytestream.StreamHost streamHost;
    private final int priority;
    private final JingleS5BTransportCandidateElement.Type type;

    public JingleS5BTransportCandidate(String candidateId,
                                       Bytestream.StreamHost streamHost,
                                       int priority,
                                       JingleS5BTransportCandidateElement.Type type) {
        this.candidateId = candidateId;
        this.streamHost = streamHost;
        this.priority = priority;
        this.type = type;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public Bytestream.StreamHost getStreamHost() {
        return streamHost;
    }

    public int getPriority() {
        return priority;
    }

    public JingleS5BTransportCandidateElement.Type getType() {
        return type;
    }

    public JingleContentTransportCandidateElement getElement() {
        return new JingleS5BTransportCandidateElement(candidateId, streamHost.getAddress(), streamHost.getJID(), streamHost.getPort(), priority, type);
    }
}
