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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransportCandidate;
import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for JingleSocks5BytestreamTransport elements.
 */
public class JingleSocks5BytestreamTransportProvider extends JingleContentTransportProvider<JingleSocks5BytestreamTransport> {

    private static final Logger LOGGER = Logger.getLogger(JingleSocks5BytestreamTransportProvider.class.getName());
    @Override
    public JingleSocks5BytestreamTransport parse(XmlPullParser parser, int initialDepth) throws Exception {
        parser.next();
        String namespace = JingleSocks5BytestreamTransport.NAMESPACE_V1;
        JingleSocks5BytestreamTransport.Builder builder = JingleSocks5BytestreamTransport.getBuilder();

        int count = parser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            LOGGER.log(Level.INFO, i + " " + parser.getNamespace() + " " + parser.getAttributeName(i));
            LOGGER.log(Level.INFO, parser.getAttributeValue(parser.getNamespace(), parser.getAttributeName(i)));
        }
        String streamId = parser.getAttributeValue(null, JingleSocks5BytestreamTransport.ATTR_SID);
        LOGGER.log(Level.INFO, "streamId: " + streamId);
        builder.setStreamId(streamId);

        String dstAddr = parser.getAttributeValue(namespace, JingleSocks5BytestreamTransport.ATTR_DSTADDR);
        LOGGER.log(Level.INFO, "dstAddr: " + dstAddr);
        builder.setDestinationAddress(dstAddr);

        String mode = parser.getAttributeValue(namespace, JingleSocks5BytestreamTransport.ATTR_MODE);
        LOGGER.log(Level.INFO, "Mode: " + mode);
        if (mode != null) {
            builder.setMode(mode.equals(udp.toString()) ? udp : tcp);
        }

        JingleSocks5BytestreamTransportCandidate.Builder cb = null;
        while (true) {
            int tag = parser.nextTag();
            String name = parser.getName();
            if (tag == START_TAG && name.equals(JingleSocks5BytestreamTransportCandidate.ELEMENT)) {
                LOGGER.log(Level.SEVERE, "Payload");
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
            }

            if (tag == END_TAG && name.equals(JingleContentTransport.ELEMENT)) {
                break;
            }
        }

        return builder.build();
    }
}
