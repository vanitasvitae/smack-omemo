package org.jivesoftware.smackx.mix.core;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;

public final class MixManager extends Manager {

    private static final Map<XMPPConnection, MixManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                MixManager.getInstanceFor(connection);
            }
        });
    }

    public static MixManager getInstanceFor(XMPPConnection connection) {
        MixManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new MixManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private MixManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(MixCoreConstants.FEATURE_CORE_1);
    }
}
