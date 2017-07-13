package org.jivesoftware.smackx.jingle_encrypted_transfer;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

/**
 * Created by vanitas on 13.07.17.
 */
public class JingleEncryptedTransferManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:jingle:jet:0";

    private static final WeakHashMap<XMPPConnection, JingleEncryptedTransferManager> INSTANCES = new WeakHashMap<>();

    private static final Map<String, JingleEncryptionMethod> encryptionProviders = new HashMap<>();

    private JingleEncryptedTransferManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleEncryptedTransferManager getInstanceFor(XMPPConnection connection) {
        JingleEncryptedTransferManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleEncryptedTransferManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }


    public void registerEncryptionProvider(String namespace, JingleEncryptionMethod provider) {
        encryptionProviders.put(namespace, provider);
    }

    public void unregisterEncryptionProvider(String namespace) {
        encryptionProviders.remove(namespace);
    }

    public JingleEncryptionMethod getEncryptionProvider(String namespace) {
        return encryptionProviders.get(namespace);
    }
}
