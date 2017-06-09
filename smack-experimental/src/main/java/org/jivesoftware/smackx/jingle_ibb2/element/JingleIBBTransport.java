package org.jivesoftware.smackx.jingle_ibb2.element;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Transport Element for JingleInBandBytestream transports.
 */
public class JingleIBBTransport extends JingleContentTransport {
    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";
    public static final String ATTR_BLOCK_SIZE = "block-size";
    public static final String ATTR_SID = "sid";

    public static final short DEFAULT_BLOCK_SIZE = 4096;

    private final short blockSize;
    private final String sid;

    public JingleIBBTransport() {
        this(DEFAULT_BLOCK_SIZE);
    }

    public JingleIBBTransport(short blockSize) {
        this(blockSize, JingleTransportManager.generateRandomId());
    }

    public JingleIBBTransport(short blockSize, String sid) {
        super(null);
        if (blockSize > 0) {
            this.blockSize = blockSize;
        } else {
            this.blockSize = DEFAULT_BLOCK_SIZE;
        }
        this.sid = sid;
    }

    public String getSessionId() {
        return sid;
    }

    public short getBlockSize() {
        return blockSize;
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.attribute(ATTR_BLOCK_SIZE, blockSize);
        xml.attribute(ATTR_SID, sid);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE_V1;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof JingleIBBTransport)) {
            return false;
        }

        return this == other || this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return this.toXML().toString().hashCode();
    }
}
