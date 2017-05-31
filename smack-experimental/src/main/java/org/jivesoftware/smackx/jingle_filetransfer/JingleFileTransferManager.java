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

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadType;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleContentDescriptionFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferPayload;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleContentDescriptionFileTransferProvider;
import org.jivesoftware.smackx.jingle_ibb.JingleInBandByteStreamManager;
import org.jivesoftware.smackx.jingle_ibb.element.JingleInBandByteStreamTransport;
import org.jxmpp.jid.FullJid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manager for Jingle File Transfers.
 *
 * @author Paul Schaub
 */
public final class JingleFileTransferManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferManager.class.getName());

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();

    /**
     * Private constructor. This registers a JingleContentDescriptionFileTransferProvider with the
     * JingleContentProviderManager.
     * @param connection connection
     */
    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentDescriptionProvider(
                NAMESPACE_V5, new JingleContentDescriptionFileTransferProvider());
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

    /**
     * QnD method.
     * @param file
     */
    public void sendFile(File file, final FullJid recipient) throws IOException, SmackException.NotConnectedException, InterruptedException {
        final byte[] bytes = DirtyHelper.readFile(file);
        if (bytes == null) {
            LOGGER.log(Level.SEVERE, "bytes are null.");
            return;
        }
        Date lastModified = new Date(file.lastModified());
        JingleFileTransferPayload payload = new JingleFileTransferPayload(
                lastModified, "A file", HashElement.fromData(HashManager.ALGORITHM.SHA_256, bytes),
                "application/octet-stream", file.getName(), (int) file.length(), null);
        ArrayList<JingleContentDescriptionPayloadType> payloadTypes = new ArrayList<>();
        payloadTypes.add(payload);

        JingleContentDescriptionFileTransfer descriptionFileTransfer = new JingleContentDescriptionFileTransfer(payloadTypes);
        final JingleInBandByteStreamTransport transport = new JingleInBandByteStreamTransport();
        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setDescription(descriptionFileTransfer)
                .addTransport(transport)
                .setCreator(JingleContent.Creator.initiator)
                .setSenders(JingleContent.Senders.initiator)
                .setName("file");
        JingleContent content = cb.build();

        final String sid = JingleInBandByteStreamManager.generateSessionId();

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setInitiator(connection().getUser())
                .setResponder(recipient)
                .setAction(JingleAction.session_initiate)
                .addJingleContent(content)
                .setSessionId(sid);
        Jingle jingle = jb.build();

        JingleManager.getInstanceFor(connection()).registerJingleSessionHandler(recipient, sid, new JingleSessionHandler() {
            @Override
            public IQ handleRequest(Jingle jingle, String sessionId) {
                if (sessionId.equals(sid)) {
                    if (jingle.getAction() == JingleAction.session_accept) {

                        InBandBytestreamSession session;
                        try {
                            session = InBandBytestreamManager.getByteStreamManager(connection())
                                    .establishSession(recipient, sid);
                        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                            LOGGER.log(Level.SEVERE, "Fail in handle request: " + e, e);
                            return null;
                        }

                        try {
                            session.getOutputStream().write(bytes);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Fail while writing: " + e, e);
                        }
                    }
                }
                return null;
            }
        });

        connection().sendStanza(jingle);
    }
}
