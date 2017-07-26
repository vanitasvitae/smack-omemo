package org.jivesoftware.smackx.jft;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jft.internal.JingleFileTransfer;
import org.jivesoftware.smackx.jft.internal.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jft.internal.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jft.provider.JingleFileTransferProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.internal.JingleContent;
import org.jivesoftware.smackx.jingle.internal.JingleSession;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transport.JingleTransportManager;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 22.07.17.
 */
public final class JingleFileTransferManager extends Manager implements JingleDescriptionManager {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final JingleManager jingleManager;

    private final List<IncomingFileTransferListener> listeners =
            Collections.synchronizedList(new ArrayList<IncomingFileTransferListener>());

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

    public OutgoingFileHandler sendFile(File file, FullJid to) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        JingleSession session = jingleManager.createSession(Role.initiator, to);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file);
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager();
        content.setTransport(transportManager.createTransport(content));

        OutgoingFileHandler handler = new OutgoingFileHandler();


        //TODO
        return handler;
    }

    public void addIncomingFileTransferListener(IncomingFileTransferListener listener) {
        listeners.add(listener);
    }

    public void removeIncomingFileTransferListener(IncomingFileTransferListener listener) {
        listeners.remove(listener);
    }

    public void notifyIncomingFileTransferListeners(JingleIncomingFileOffer offer) {
        for (IncomingFileTransferListener l : listeners) {
            l.onIncomingFileTransfer(new IncomingFileTransferCallback(offer));
        }
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
