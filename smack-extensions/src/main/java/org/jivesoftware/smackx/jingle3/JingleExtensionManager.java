package org.jivesoftware.smackx.jingle3;

import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle3.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle3.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle3.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle3.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle3.provider.JingleContentSecurityProvider;
import org.jivesoftware.smackx.jingle3.provider.JingleContentTransportProvider;

/**
 * Created by vanitas on 18.07.17.
 */
public final class JingleExtensionManager extends Manager {

    private static final WeakHashMap<XMPPConnection, JingleExtensionManager> INSTANCES = new WeakHashMap<>();

    private static final WeakHashMap<String, JingleContentDescriptionProvider<?>> descriptionProviders = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleContentTransportProvider<?>> transportProviders = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleContentSecurityProvider<?>> securityProviders = new WeakHashMap<>();

    private static final WeakHashMap<String, JingleDescriptionAdapter<?>> descriptionAdapters = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleTransportAdapter<?>> transportAdapters = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleSecurityAdapter<?>> securityAdapters = new WeakHashMap<>();

    public final WeakHashMap<String, JingleDescriptionManager> descriptionManagers = new WeakHashMap<>();
    public final WeakHashMap<String, JingleTransportManager> transportManagers = new WeakHashMap<>();
    public final WeakHashMap<String, JingleSecurityManager> securityManagers = new WeakHashMap<>();

    private JingleExtensionManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleExtensionManager getInstanceFor(XMPPConnection connection) {
        JingleExtensionManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleExtensionManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public static void registerDescriptionProvider(JingleContentDescriptionProvider<?> provider) {
        descriptionProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentDescriptionProvider<?> getDescriptionProvider(String namespace) {
        return descriptionProviders.get(namespace);
    }

    public static void registerTransportProvider(JingleContentTransportProvider<?> provider) {
        transportProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentTransportProvider<?> getTransportProvider(String namespace) {
        return transportProviders.get(namespace);
    }

    public static void registerSecurityProvider(JingleContentSecurityProvider<?> provider) {
        securityProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentSecurityProvider<?> getSecurityProvider(String namespace) {
        return securityProviders.get(namespace);
    }

    public static void addJingleDescriptionAdapter(JingleDescriptionAdapter<?> adapter) {
        descriptionAdapters.put(adapter.getNamespace(), adapter);
    }

    public static void addJingleTransportAdapter(JingleTransportAdapter<?> adapter) {
        transportAdapters.put(adapter.getNamespace(), adapter);
    }

    public static void addJingleSecurityAdapter(JingleSecurityAdapter<?> adapter) {
        securityAdapters.put(adapter.getNamespace(), adapter);
    }

    public static JingleDescriptionAdapter<?> getJingleDescriptionAdapter(String namespace) {
        return descriptionAdapters.get(namespace);
    }

    public static JingleTransportAdapter<?> getJingleTransportAdapter(String namespace) {
        return transportAdapters.get(namespace);
    }

    public static JingleSecurityAdapter<?> getJingleSecurityAdapter(String namespace) {
        return securityAdapters.get(namespace);
    }

    public void addJingleDescriptionManager(JingleDescriptionManager manager) {
        descriptionManagers.put(manager.getNamespace(), manager);
    }

    public JingleDescriptionManager getDescriptionManager(String namespace) {
        return descriptionManagers.get(namespace);
    }

    public void addJingleTransportManager(JingleTransportManager manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    public JingleTransportManager getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    public void addJingleSecurityManager(JingleSecurityManager manager) {
        securityManagers.put(manager.getNamespace(), manager);
    }

    public JingleSecurityManager getSecurityManager(String namespace) {
        return securityManagers.get(namespace);
    }
}
