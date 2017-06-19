package org.jivesoftware.smackx.jingle;

import java.util.HashMap;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;

/**
 * Created by vanitas on 19.06.17.
 */
public final class JingleTransportMethodManager extends Manager {

    private static final WeakHashMap<XMPPConnection, JingleTransportMethodManager> INSTANCES = new WeakHashMap<>();
    private final HashMap<String, JingleTransportManager<?>> transportManagers = new HashMap<>();

    private JingleTransportMethodManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleTransportMethodManager getInstanceFor(XMPPConnection connection) {
        JingleTransportMethodManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleTransportMethodManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void registerTransportManager(JingleTransportManager<?> manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    public JingleTransportManager<?> getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    public JingleTransportManager<?> getTransportManager(Jingle request) {
        JingleContent content = request.getContents().get(0);
        JingleContentTransport transport = content.getJingleTransports().get(0);
        return getTransportManager(transport.getNamespace());
    }
}
