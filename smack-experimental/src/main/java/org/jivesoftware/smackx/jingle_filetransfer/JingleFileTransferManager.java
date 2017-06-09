/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.exception.UnsupportedJingleTransportException;
import org.jivesoftware.smackx.jingle_filetransfer.callback.JingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferContentDescription;
import org.jivesoftware.smackx.jingle_filetransfer.handler.ResponderIncomingFileTransferAccepted;
import org.jivesoftware.smackx.jingle_filetransfer.handler.InitiatorOutgoingFileTransferInitiated;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingJingleFileTransferListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_ibb.JingleIBBTransportManager;
import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle File Transfers.
 *
 * @author Paul Schaub
 */
public final class JingleFileTransferManager extends Manager implements JingleHandler {

    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferManager.class.getName());

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";

    private final JingleManager jingleManager;
    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final HashSet<IncomingJingleFileTransferListener> incomingJingleFileTransferListeners = new HashSet<>();

    /**
     * Private constructor. This registers a JingleContentDescriptionFileTransferProvider with the
     * JingleContentProviderManager.
     * @param connection connection
     */
    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE_V5);
        jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(
                NAMESPACE_V5, this);
        JingleContentProviderManager.addJingleContentDescriptionProvider(
                NAMESPACE_V5, new JingleFileTransferContentDescriptionProvider());
        JingleIBBTransportManager.getInstanceFor(connection);
        //JingleInBandBytestreamTransportManager.getInstanceFor(connection);
        //JingleSocks5BytestreamTransportManager.getInstanceFor(connection);
    }

    /**
     * Return a new instance of the FileTransferManager for the given connection.
     *
     * @param connection XMPPConnection we wish to get a FileTransferManager for.
     * @return manager instance.
     */
    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void addIncomingFileTransferListener(IncomingJingleFileTransferListener listener) {
        incomingJingleFileTransferListeners.add(listener);
    }

    public void removeIncomingFileTransferListener(IncomingJingleFileTransferListener listener) {
        incomingJingleFileTransferListeners.remove(listener);
    }

    void notifyIncomingFileTransferListeners(Jingle jingle, JingleFileTransferCallback callback) {
        for (IncomingJingleFileTransferListener l : incomingJingleFileTransferListeners) {
            l.onIncomingJingleFileTransfer(jingle, callback);
        }
    }

    public void addFileTransferRejectedListener() {

    }

    /**
     * QnD method.
     * @param file
     */
    public void sendFile(File file, final FullJid recipient) throws IOException, SmackException, InterruptedException, XMPPException {
        AbstractJingleTransportManager<?> tm = JingleTransportManager.getInstanceFor(connection())
                .getAvailableJingleBytestreamManagers().iterator().next();

        JingleFileTransferChild.Builder b = JingleFileTransferChild.getBuilder();
        b.setFile(file);
        b.setDescription("File");
        b.setMediaType("application/octet-stream");

        JingleFileTransferContentDescription description = new JingleFileTransferContentDescription(
                Collections.singletonList((JingleContentDescriptionChildElement) b.build()));
        Jingle initiate = tm.createSessionInitiate(recipient, description);

        JingleManager.FullJidAndSessionId fullJidAndSessionId =
                new JingleManager.FullJidAndSessionId(recipient, initiate.getSid());

        jingleManager.registerJingleSessionHandler(recipient, initiate.getSid(),
                new InitiatorOutgoingFileTransferInitiated(this, fullJidAndSessionId, file));

        connection().sendStanza(initiate);
    }

    public FullJid ourJid() {
        return connection().getUser();
    }

    @Override
    public IQ handleJingleSessionInitiate(final Jingle jingle) {
        if (jingle.getAction() != JingleAction.session_initiate) {
            //TODO tie-break?
            return null;
        }

        JingleTransportManager tm = JingleTransportManager.getInstanceFor(connection());
        String transportNamespace = jingle.getContents().get(0).getJingleTransports().get(0).getNamespace();

        AbstractJingleTransportManager<?> transportManager = null;
        for (AbstractJingleTransportManager<?> b : tm.getAvailableJingleBytestreamManagers()) {
            if (b.getNamespace().equals(transportNamespace)) {
                transportManager = b;
            }
        }

        if (transportManager == null) {
            //TODO unsupported-transport?
            return null;
        }

        final AbstractJingleTransportManager<?> finalTransportManager = transportManager;

        notifyIncomingFileTransferListeners(jingle, new JingleFileTransferCallback() {
            @Override
            public void accept(File target) throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, UnsupportedJingleTransportException, SmackException.NoResponseException {
                connection().sendStanza(finalTransportManager.createSessionAccept(jingle));
                ResponderIncomingFileTransferAccepted responded = new ResponderIncomingFileTransferAccepted(JingleFileTransferManager.this, jingle, target);
                jingleManager.registerJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSid(),
                        responded);
                finalTransportManager.setIncomingRespondedSessionListener(jingle, responded);
            }

            @Override
            public void decline() throws SmackException.NotConnectedException, InterruptedException {
                //TODO
            }
        });

        return IQ.createResultIQ(jingle);
    }

    public XMPPConnection getConnection() {
        return connection();
    }
}
