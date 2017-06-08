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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle.AbstractJingleContentTransportManager;
import org.jivesoftware.smackx.jingle.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingJingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferContentDescription;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingJingleFileTransferListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_ibb.JingleInBandBytestreamTransportManager;
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
        JingleInBandBytestreamTransportManager.getInstanceFor(connection);
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

    void notifyIncomingFileTransferListeners(Jingle jingle, IncomingJingleFileTransferCallback callback) {
        for (IncomingJingleFileTransferListener l : incomingJingleFileTransferListeners) {
            l.onIncomingJingleFileTransfer(jingle, callback);
        }
    }

    /**
     * QnD method.
     * @param file
     */
    public void sendFile(File file, final FullJid recipient) throws IOException, SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException, SmackException.NoResponseException {
        AbstractJingleContentTransportManager<?> preferedTransportManager = JingleInBandBytestreamTransportManager.getInstanceFor(connection());

        JingleFileTransferSession session = new JingleFileTransferSession(connection(), recipient, connection().getUser(), recipient);
        JingleFileTransferChildElement.Builder b = JingleFileTransferChildElement.getBuilder();
        b.setFile(file);
        byte[] buf = new byte[(int) file.length()];
        HashElement hashElement = FileAndHashReader.readAndCalculateHash(file, buf, HashManager.ALGORITHM.SHA_256);
        b.setHash(hashElement);
        b.setDescription("File");
        b.setMediaType("text/plain");

        session.setBytes(buf);
        JingleManager.getInstanceFor(connection()).registerJingleSession(session);

        ArrayList<JingleContentDescriptionChildElement> payloads = new ArrayList<>();
        payloads.add(b.build());

        JingleContent.Builder bb = JingleContent.getBuilder();
        bb.setDescription(new JingleFileTransferContentDescription(payloads))
                .setCreator(JingleContent.Creator.initiator)
                .setName(StringUtils.randomString(24))
                .addTransport(preferedTransportManager.createJingleContentTransport());

        Jingle jingle = (Jingle) session.initiate(Collections.singletonList(bb.build()));
        jingle.setTo(recipient);
        connection().sendStanza(jingle);
    }

    public FullJid ourJid() {
        return connection().getUser();
    }

    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        JingleFileTransferSession session = new JingleFileTransferSession(connection(), jingle);
        JingleManager.getInstanceFor(connection()).registerJingleSession(session);
        return session.handleRequest(jingle);
    }
}
