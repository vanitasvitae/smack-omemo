package org.jivesoftware.smackx.jft;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.controller.OutgoingFileRequestController;
import org.jivesoftware.smackx.jft.internal.AbstractJingleFileTransfer;
import org.jivesoftware.smackx.jft.internal.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jft.internal.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jft.internal.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jft.internal.JingleOutgoingFileRequest;
import org.jivesoftware.smackx.jft.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jft.listener.IncomingFileRequestListener;
import org.jivesoftware.smackx.jft.provider.JingleFileTransferProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.util.Role;
import org.jivesoftware.smackx.jingle.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.components.JingleContent;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 22.07.17.
 */
public final class JingleFileTransferManager extends Manager implements JingleDescriptionManager {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final JingleManager jingleManager;

    private final List<IncomingFileOfferListener> offerListeners =
            Collections.synchronizedList(new ArrayList<IncomingFileOfferListener>());
    private final List<IncomingFileRequestListener> requestListeners =
            Collections.synchronizedList(new ArrayList<IncomingFileRequestListener>());

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

    public OutgoingFileOfferController sendFile(File file, FullJid to) {
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

        //TODO
        return offer;
    }

    public OutgoingFileRequestController requestFile() {
        JingleOutgoingFileRequest request = new JingleOutgoingFileRequest();

        //TODO at some point.

        return request;
    }

    public void addIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.add(listener);
    }

    public void removeIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.remove(listener);
    }

    public void notifyIncomingFileOfferListeners(JingleIncomingFileOffer offer) {
        for (IncomingFileOfferListener l : offerListeners) {
            l.onIncomingFileTransfer(offer);
        }
    }

    public void addIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.add(listener);
    }

    public void removeIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.remove(listener);
    }

    public void notifyIncomingFileRequestListeners(JingleIncomingFileRequest request) {
        for (IncomingFileRequestListener l : requestListeners) {
            l.onIncomingFileRequest(request);
        }
    }

    @Override
    public String getNamespace() {
        return AbstractJingleFileTransfer.NAMESPACE;
    }

    @Override
    public JingleElement notifyContentListeners(JingleContent content, ContentAddCallback callback) {

    }
}
