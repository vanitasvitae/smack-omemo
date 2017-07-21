package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.internal.Content;
import org.jivesoftware.smackx.jingle.internal.Transport;

/**
 * Created by vanitas on 21.07.17.
 */
public class JingleIBBTransportManager extends Manager implements JingleTransportManager {

    public static final short MAX_BLOCKSIZE = 8192;

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }

    @Override
    public Transport<?> createTransport(Content content) {
        return new JingleIBBTransport();
    }

    @Override
    public Transport<?> createTransport(Content content, Transport<?> peersTransport) {
        JingleIBBTransport other = (JingleIBBTransport) peersTransport;
        return new JingleIBBTransport(other.getSid(), (short) Math.min(other.getBlockSize(), MAX_BLOCKSIZE));
    }

    @Override
    public int compareTo(JingleTransportManager manager) {
        return 0;
    }
}
