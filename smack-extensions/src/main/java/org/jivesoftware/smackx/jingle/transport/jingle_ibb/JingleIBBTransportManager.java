package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.internal.JingleContent;
import org.jivesoftware.smackx.jingle.internal.JingleTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.provider.JingleIBBTransportProvider;

/**
 * Created by vanitas on 21.07.17.
 */
public class JingleIBBTransportManager extends Manager implements JingleTransportManager {

    public static final short MAX_BLOCKSIZE = 8192;

    private static final WeakHashMap<XMPPConnection, JingleIBBTransportManager> INSTANCES = new WeakHashMap<>();

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.addJingleTransportManager(this);
        JingleContentProviderManager.addJingleContentTransportProvider(getNamespace(), new JingleIBBTransportProvider());
    }

    public static JingleIBBTransportManager getInstanceFor(XMPPConnection connection) {
        JingleIBBTransportManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleIBBTransportManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }

    @Override
    public JingleTransport<?> createTransport(JingleContent content) {
        return new JingleIBBTransport();
    }

    @Override
    public JingleTransport<?> createTransport(JingleContent content, JingleTransport<?> peersTransport) {
        JingleIBBTransport other = (JingleIBBTransport) peersTransport;
        return new JingleIBBTransport(other.getSid(), (short) Math.min(other.getBlockSize(), MAX_BLOCKSIZE));
    }

    @Override
    public int compareTo(JingleTransportManager manager) {
        return -1; // We are literally the worst.
    }
}
