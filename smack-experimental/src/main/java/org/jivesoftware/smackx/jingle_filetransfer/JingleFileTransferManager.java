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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle_filetransfer.callback.JingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferContentDescription;
import org.jivesoftware.smackx.jingle_filetransfer.handler.InitiatorOutgoingFileTransferInitiated;
import org.jivesoftware.smackx.jingle_filetransfer.handler.ResponderIncomingFileTransferAccepted;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingJingleFileTransferListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_s5b.JingleS5BTransportManager;
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
        //JingleIBBTransportManager.getInstanceFor(connection);
        JingleS5BTransportManager.getInstanceFor(connection);
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

    /**
     * QnD method.
     * @param file
     */
    public void sendFile(final File file, final FullJid recipient) throws Exception {
        AbstractJingleTransportManager<?> tm = JingleTransportManager.getInstanceFor(connection())
                .getAvailableJingleBytestreamManagers().iterator().next();

        JingleFileTransferChild.Builder b = JingleFileTransferChild.getBuilder();
        b.setFile(file);
        b.setDescription("File");
        b.setMediaType("application/octet-stream");

        JingleFileTransferContentDescription description = new JingleFileTransferContentDescription(
                Collections.singletonList((JingleContentDescriptionChildElement) b.build()));

        JingleContentTransport transport = tm.createJingleContentTransport(recipient);
        Jingle initiate = sessionInitiate(recipient, description, transport);

        JingleManager.FullJidAndSessionId fullJidAndSessionId =
                new JingleManager.FullJidAndSessionId(recipient, initiate.getSessionId());

        InitiatorOutgoingFileTransferInitiated sessionHandler =
                new InitiatorOutgoingFileTransferInitiated(this, fullJidAndSessionId, file);

        jingleManager.registerJingleSessionHandler(recipient, initiate.getSessionId(), sessionHandler);

        JingleTransportHandler<?> transportHandler = tm.createJingleTransportHandler(sessionHandler);
        connection().sendStanza(initiate);
        transportHandler.establishOutgoingSession(initiate, new JingleTransportEstablishedCallback() {
            @Override
            public void onSessionEstablished(BytestreamSession bytestreamSession) {
                try {
                    byte[] filebuf = new byte[(int) file.length()];
                    HashElement hashElement = FileAndHashReader.readAndCalculateHash(file, filebuf, HashManager.ALGORITHM.SHA_256);
                    bytestreamSession.getInputStream().read(filebuf);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bytestreamSession.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onSessionFailure(JingleTransportFailureException reason) {

            }
        });
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

        notifyIncomingFileTransferListeners(jingle, new JingleFileTransferCallback() {
            @Override
            public void acceptFileTransfer(final File target) throws Exception {
                connection().sendStanza(sessionAccept(jingle));
                ResponderIncomingFileTransferAccepted responded = new ResponderIncomingFileTransferAccepted(
                        JingleFileTransferManager.this, jingle, target);
                jingleManager.registerJingleSessionHandler(jingle.getFrom().asFullJidIfPossible(), jingle.getSessionId(),
                        responded);
            }

            @Override
            public void declineFileTransfer() throws SmackException.NotConnectedException, InterruptedException {
                connection().sendStanza(decline(jingle));
            }
        });

        return IQ.createResultIQ(jingle);
    }

    public Jingle sessionInitiate(FullJid recipient, JingleContentDescription contentDescription, JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(StringUtils.randomString(24))
                .setAction(JingleAction.session_initiate)
                .setInitiator(connection().getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setDescription(contentDescription)
                .setName(StringUtils.randomString(24))
                .setCreator(JingleContent.Creator.initiator)
                .setSenders(JingleContent.Senders.initiator)
                .addTransport(transport);

        jb.addJingleContent(cb.build());
        Jingle jingle = jb.build();
        jingle.setFrom(connection().getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public Jingle sessionAccept(Jingle request) throws Exception {
        JingleContent content = request.getContents().get(0);

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(request.getSessionId())
                .setAction(JingleAction.session_accept)
                .setResponder(connection().getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setSenders(content.getSenders())
                .setCreator(content.getCreator())
                .setName(content.getName())
                .setDescription(content.getDescription());

        AbstractJingleTransportManager<?> tm = JingleTransportManager.getInstanceFor(connection())
                .getJingleContentTransportManager(request);

        JingleContentTransport transport = tm.createJingleContentTransport(request);
        cb.addTransport(transport);

        jb.addJingleContent(cb.build());
        Jingle jingle = jb.build();
        jingle.setFrom(connection().getUser());
        jingle.setTo(request.getFrom());

        return jingle;
    }

    public Jingle decline(Jingle request) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setResponder(connection().getUser())
                .setAction(JingleAction.session_terminate)
                .setReason(JingleReason.Reason.decline)
                .setSessionId(request.getSessionId());
        Jingle jingle = jb.build();
        jingle.setTo(request.getFrom());
        jingle.setFrom(connection().getUser());
        return jingle;
    }

    public void receiveFile(Jingle request, BytestreamSession session, File target) {
        JingleFileTransferChild file = (JingleFileTransferChild)
                request.getContents().get(0).getDescription().getJingleContentDescriptionChildren().get(0);

        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            if (!target.exists()) {
                target.createNewFile();
            }

            inputStream = session.getInputStream();
            outputStream = new FileOutputStream(target);

            byte[] fileBuf = new byte[file.getSize()];
            byte[] buf = new byte[2048];
            int read = 0;
            while (read < fileBuf.length) {
                int r = inputStream.read(buf);
                if (r >= 0) {
                    System.arraycopy(buf, 0, fileBuf, read, r);
                    read += r;
                }
            }

            outputStream.write(fileBuf);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                session.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public XMPPConnection getConnection() {
        return connection();
    }
}
