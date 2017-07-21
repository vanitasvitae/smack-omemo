package org.jivesoftware.smackx.jft;

import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jft.internal.JingleFileTransfer;
import org.jivesoftware.smackx.jft.provider.JingleFileTransferProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.internal.JingleContent;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;

/**
 * Created by vanitas on 22.07.17.
 */
public final class JingleFileTransferManager extends Manager implements JingleDescriptionManager {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final JingleManager jingleManager;

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.addJingleDescriptionManager(this);
        JingleContentProviderManager.addJingleContentDescriptionProvider(getNamespace(), new JingleFileTransferProvider());
    }

    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }

    @Override
    public JingleElement notifyContentListeners(JingleContent content, ContentAddCallback callback) {
        return null;
    }
}
