package org.jivesoftware.smackx.jingle_s5b.elements;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;

/**
 * Socks5Bytestream transport element.
 */
public class JingleSocks5BytestreamTransport extends JingleContentTransport {
    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:s5b:1";
    public static final String ATTR_DSTADDR = "dstaddr";
    public static final String ATTR_MODE = "mode";
    public static final String ATTR_SID = "sid";

    private final String streamId;
    private final String dstAddr;
    private final Bytestream.Mode mode;

    protected JingleSocks5BytestreamTransport(List<JingleContentTransportCandidate> candidates, String streamId, String dstAddr, Bytestream.Mode mode) {
        super(candidates);
        this.streamId = streamId;
        this.dstAddr = dstAddr;
        this.mode = mode;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getDestinationAddress() {
        return dstAddr;
    }

    public Bytestream.Mode getMode() {
        return mode;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE_V1;
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.attribute(ATTR_DSTADDR, dstAddr);
        xml.attribute(ATTR_MODE, mode.toString());
        xml.attribute(ATTR_SID, streamId);
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String streamId;
        private String dstAddr;
        private Bytestream.Mode mode = Bytestream.Mode.tcp;
        private ArrayList<JingleContentTransportCandidate> candidates = new ArrayList<>();

        public Builder setStreamId(String sid) {
            this.streamId = sid;
            return this;
        }

        public Builder setDestinationAddress(String dstAddr) {
            this.dstAddr = dstAddr;
            return this;
        }

        public Builder setMode(Bytestream.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder addTransportCandidate(JingleSocks5BytestreamTransportCandidate candidate) {
            this.candidates.add(candidate);
            return this;
        }

        public JingleSocks5BytestreamTransport build() {
            StringUtils.requireNotNullOrEmpty(streamId, "sid MUST be neither null, nor empty.");
            return new JingleSocks5BytestreamTransport(candidates, streamId, dstAddr, mode);
        }
    }
}
