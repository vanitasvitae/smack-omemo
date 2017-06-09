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
package org.jivesoftware.smackx.jingle_s5b.provider;

import static org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.Mode.tcp;
import static org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.Mode.udp;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_CID;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_HOST;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_JID;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_PORT;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_PRIORITY;
import static org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate.ATTR_TYPE;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportInfo;
import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for JingleSocks5BytestreamTransport elements.
 */
public class JingleSocks5BytestreamTransportProvider extends JingleContentTransportProvider<JingleSocks5BytestreamTransport> {

    @Override
    public JingleSocks5BytestreamTransport parse(XmlPullParser parser, int initialDepth) throws Exception {
        JingleSocks5BytestreamTransport.Builder builder = JingleSocks5BytestreamTransport.getBuilder();

        String streamId = parser.getAttributeValue(null, JingleSocks5BytestreamTransport.ATTR_SID);
        builder.setStreamId(streamId);

        String dstAddr = parser.getAttributeValue(null, JingleSocks5BytestreamTransport.ATTR_DSTADDR);
        builder.setDestinationAddress(dstAddr);

        String mode = parser.getAttributeValue(null, JingleSocks5BytestreamTransport.ATTR_MODE);
        if (mode != null) {
            builder.setMode(mode.equals(udp.toString()) ? udp : tcp);
        }

        JingleSocks5BytestreamTransportCandidate.Builder cb;
        while (true) {
            int tag = parser.nextTag();
            String name = parser.getName();
            if (tag == START_TAG) {
                switch (name) {

                    case JingleSocks5BytestreamTransportCandidate.ELEMENT:
                    cb = JingleSocks5BytestreamTransportCandidate.getBuilder();
                    cb.setCandidateId(parser.getAttributeValue(null, ATTR_CID));
                    cb.setHost(parser.getAttributeValue(null, ATTR_HOST));
                    cb.setJid(parser.getAttributeValue(null, ATTR_JID));
                    cb.setPriority(Integer.parseInt(parser.getAttributeValue(null, ATTR_PRIORITY)));

                    String portString = parser.getAttributeValue(null, ATTR_PORT);
                    if (portString != null) {
                        cb.setPort(Integer.parseInt(portString));
                    }

                    String typeString = parser.getAttributeValue(null, ATTR_TYPE);
                    if (typeString != null) {
                        cb.setType(JingleSocks5BytestreamTransportCandidate.Type.fromString(typeString));
                    }
                    builder.addTransportCandidate(cb.build());
                    break;

                    case JingleSocks5BytestreamTransportInfo.CandidateActivated.ELEMENT:
                        builder.addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateActivated(
                                parser.getAttributeValue(null,
                                        JingleSocks5BytestreamTransportInfo.CandidateActivated.ATTR_CID)));
                        break;

                    case JingleSocks5BytestreamTransportInfo.CandidateUsed.ELEMENT:
                        builder.addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateUsed(
                                parser.getAttributeValue(null,
                                        JingleSocks5BytestreamTransportInfo.CandidateUsed.ATTR_CID)));
                        break;

                    case JingleSocks5BytestreamTransportInfo.CandidateError.ELEMENT:
                        builder.addTransportInfo(JingleSocks5BytestreamTransportInfo.CandidateError());
                        break;

                    case JingleSocks5BytestreamTransportInfo.ProxyError.ELEMENT:
                        builder.addTransportInfo(JingleSocks5BytestreamTransportInfo.ProxyError());
                        break;
                }
            }

            if (tag == END_TAG && name.equals(JingleContentTransport.ELEMENT)) {
                break;
            }
        }

        return builder.build();
    }
}
