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
package org.jivesoftware.smackx.jingle_s5b.elements;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;

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

    protected JingleSocks5BytestreamTransport(List<JingleContentTransportCandidate> candidates, List<JingleContentTransportInfo> infos, String streamId, String dstAddr, Bytestream.Mode mode) {
        super(candidates, infos);
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
        return mode == null ? Bytestream.Mode.tcp : mode;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE_V1;
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.optAttribute(ATTR_DSTADDR, dstAddr);
        xml.optAttribute(ATTR_MODE, mode);
        xml.attribute(ATTR_SID, streamId);
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String streamId;
        private String dstAddr;
        private Bytestream.Mode mode;
        private ArrayList<JingleContentTransportCandidate> candidates = new ArrayList<>();
        private ArrayList<JingleContentTransportInfo> infos = new ArrayList<>();

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

        public Builder addTransportInfo(JingleContentTransportInfo info) {
            this.infos.add(info);
            return this;
        }

        public Builder setCandidateUsed(String candidateId) {
            return addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateUsed(candidateId));
        }

        public Builder setCandidateActivated(String candidateId) {
            return addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateActivated(candidateId));
        }

        public Builder setCandidateError() {
            return addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateError());
        }

        public Builder setProxyError() {
            return addTransportInfo(JingleSocks5BytestreamTransportInfo.ProxyError());
        }

        public JingleSocks5BytestreamTransport build() {
            StringUtils.requireNotNullOrEmpty(streamId, "sid MUST be neither null, nor empty.");
            return new JingleSocks5BytestreamTransport(candidates, infos, streamId, dstAddr, mode);
        }
    }
}
