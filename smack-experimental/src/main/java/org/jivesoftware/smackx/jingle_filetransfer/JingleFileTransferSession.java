/**
 *
 * Copyright © 2017 Paul Schaub
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
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.AbstractJingleTransportManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedJingleTransportException;
import org.jivesoftware.smackx.jingle_filetransfer.callback.JingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferContentDescription;
import org.jxmpp.jid.FullJid;

/**
 * JingleSession.
 */
public class JingleFileTransferSession extends AbstractJingleSession {

    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferSession.class.getName());

    private final File source;
    private File target;
    private final JingleContent proposedContent;
    private final FullJid remote;
    private final String sessionId;

    /**
     * Send a file.
     * @param connection
     * @param send
     */
    public JingleFileTransferSession(XMPPConnection connection, FullJid recipient, File send) {
        super(connection);
        this.remote = recipient;
        this.source = send;
        this.sessionId = StringUtils.randomString(24);

        JingleFileTransferChild fileTransferChild = fileElementFromFile(send);
        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setSenders(JingleContent.Senders.initiator)
                .setCreator(JingleContent.Creator.initiator)
                .setName(StringUtils.randomString(24))
                .setDescription(new JingleFileTransferContentDescription(
                        Collections.singletonList((JingleContentDescriptionChildElement) fileTransferChild)));
        try {
            cb.addTransport(defaultTransport());
        } catch (Exception e) {
            throw new AssertionError("At least IBB should work. " + e);
        }

        this.proposedContent = cb.build();

        try {
            connection.sendStanza(createFileOffer());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not send session-initiate: " + e, e);
            return;
        }

        this.state = new OutgoingInitiated(connection, this);
    }

    /**
     * Receive a file.
     * @param connection
     */
    public JingleFileTransferSession(XMPPConnection connection, Jingle initiate) {
        super(connection);
        LOGGER.log(Level.INFO, "Incoming session!");
        this.sessionId = initiate.getSessionId();
        this.remote = initiate.getInitiator();
        this.source = null;
        this.proposedContent = initiate.getContents().get(0);

        this.state = new IncomingFresh(connection, this);
    }

    /**
     * session-initiate has been sent.
     */
    public static class OutgoingInitiated extends AbstractJingleSession {

        private final JingleFileTransferSession parent;

        public OutgoingInitiated(XMPPConnection connection, JingleFileTransferSession parent) {
            super(connection);
            this.parent = parent;
        }

        @Override
        protected IQ handleSessionAccept(Jingle jingle) {
            parent.state = new OutgoingAccepted(connection, parent);
            //TODO: Notify parent
            return IQ.createResultIQ(jingle);
        }

        @Override
        protected IQ handleSessionTerminate(Jingle jingle) {
            //TODO: notify client
            JingleFileTransferManager.getInstanceFor(connection).removeJingleSession(parent);
            return IQ.createResultIQ(jingle);
        }

    }

    public static class OutgoingAccepted extends AbstractJingleSession {
        private final JingleFileTransferSession parent;

        public OutgoingAccepted(XMPPConnection connection, final JingleFileTransferSession parent) {
            super(connection);
            this.parent = parent;

            AbstractJingleTransportManager<?> tm;
            try {
                tm = JingleTransportManager.getJingleContentTransportManager(connection,
                        parent.proposedContent.getJingleTransports().get(0));
            } catch (UnsupportedJingleTransportException e) {
                throw new AssertionError("Since we initiated the transport, we MUST know the transport method.");
            }

            JingleTransportHandler<?> transportHandler = tm.createJingleTransportHandler(this);
            transportHandler.establishOutgoingSession(parent.getFullJidAndSessionId(),
                    parent.proposedContent, parent.outgoingFileTransferSessionEstablishedCallback);
        }
    }

    public static class IncomingFresh extends AbstractJingleSession {
        private final JingleFileTransferSession parent;

        public IncomingFresh(XMPPConnection connection, JingleFileTransferSession parent) {
            super(connection);
            this.parent = parent;
        }

        @Override
        protected IQ handleSessionInitiate(final Jingle initiate) {
            if (initiate.getAction() != JingleAction.session_initiate) {
                throw new IllegalArgumentException("Jingle action MUST be session-initiate!");
            }

            //Get <file/>
            JingleFileTransferChild file = (JingleFileTransferChild) initiate.getContents().get(0)
                    .getDescription().getJingleContentDescriptionChildren().get(0);

            final JingleFileTransferCallback callback = new JingleFileTransferCallback() {
                @Override
                public void acceptFileTransfer(File target) throws SmackException.NotConnectedException, InterruptedException {
                    Jingle response = null;
                    try {
                        response = parent.createSessionAccept(initiate);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Could not create accept-session stanza: " + e, e);
                    }

                    connection.sendStanza(response);
                    parent.target = target;
                    parent.state = new IncomingAccepted(connection, parent);
                }

                @Override
                public void declineFileTransfer() throws SmackException.NotConnectedException, InterruptedException {
                    connection.sendStanza(parent.createSessionDecline(initiate));
                    parent.state = null;
                    JingleFileTransferManager.getInstanceFor(connection).removeJingleSession(parent);
                }
            };

            //Set callback
            JingleFileTransferManager.getInstanceFor(connection).notifyIncomingJingleFileTransferListeners(file, callback);
            return IQ.createResultIQ(initiate);
        }
    }

    public static class IncomingAccepted extends AbstractJingleSession {

        public IncomingAccepted(XMPPConnection connection, JingleFileTransferSession parent) {
            super(connection);
            AbstractJingleTransportManager<?> tm = null;
            try {
                tm = JingleTransportManager.getJingleContentTransportManager(
                        connection, parent.proposedContent.getJingleTransports().get(0));
            } catch (UnsupportedJingleTransportException e) {
                throw new AssertionError("Since we accepted the transfer, we MUST know the transport method.");
            }

            JingleTransportHandler<?> transportHandler = tm.createJingleTransportHandler(this);
            transportHandler.establishIncomingSession(parent.getFullJidAndSessionId(),
                    parent.proposedContent, parent.incomingFileTransferSessionEstablishedCallback);
        }
    }

    protected Jingle createFileOffer() throws Exception {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_initiate)
                .setInitiator(connection.getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setSenders(JingleContent.Senders.initiator)
                .setCreator(JingleContent.Creator.initiator)
                .setName(StringUtils.randomString(24));

        cb.setDescription(new JingleFileTransferContentDescription(Collections.singletonList(
                proposedContent.getDescription().getJingleContentDescriptionChildren().get(0))));

        cb.addTransport(proposedContent.getJingleTransports().get(0));
        jb.addJingleContent(cb.build());

        Jingle jingle = jb.build();
        jingle.setTo(remote);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    protected Jingle createSessionAccept(Jingle jingle) throws Exception {
        JingleContent content = jingle.getContents().get(0);

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_accept)
                .setResponder(connection.getUser())
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();

        AbstractJingleTransportManager<?> tm;
        try {
            tm = JingleTransportManager.getJingleContentTransportManager(
                    connection, jingle);
        } catch (UnsupportedJingleTransportException e) {
            throw new AssertionError("Should never happen."); //TODO: Make sure.
        }

        cb.addTransport(tm.createJingleContentTransport(jingle))
                .setDescription(content.getDescription())
                .setName(content.getName())
                .setCreator(content.getCreator())
                .setSenders(content.getSenders());

        jb.addJingleContent(cb.build());
        Jingle accept = jb.build();
        accept.setTo(remote);
        accept.setFrom(connection.getUser());

        return accept;
    }

    public Jingle createSessionDecline(Jingle initiate) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setReason(JingleReason.Reason.decline)
                .setResponder(connection.getUser())
                .setSessionId(sessionId);

        return jb.build();
    }

    static JingleFileTransferChild fileElementFromFile(File file) {
        JingleFileTransferChild.Builder fb = JingleFileTransferChild.getBuilder();
        fb.setFile(file)
                .setDescription("A File")
                .setMediaType("application/octetStream");

        return fb.build();
    }

    JingleContentTransport defaultTransport() {
        JingleTransportManager transportManager = JingleTransportManager.getInstanceFor(connection);
        JingleContentTransport transport = null;
        Iterator<AbstractJingleTransportManager<?>> iterator =
                transportManager.getAvailableJingleBytestreamManagers().iterator();

        while (transport == null && iterator.hasNext()) {
            AbstractJingleTransportManager<?> tm = iterator.next();
            try {
                transport = tm.createJingleContentTransport(remote);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not create JingleContentTransport " + tm.getNamespace() +
                        ". Skip.");
            }
        }

        return transport;
    }

    protected JingleTransportEstablishedCallback outgoingFileTransferSessionEstablishedCallback =
            new JingleTransportEstablishedCallback() {
                @Override
                public void onSessionEstablished(BytestreamSession bytestreamSession) {
                    send(bytestreamSession);
                }

                @Override
                public void onSessionFailure(JingleTransportFailureException reason) {
                    //TODO: Send transport-failed or so.
                }
            };

    protected JingleTransportEstablishedCallback incomingFileTransferSessionEstablishedCallback =
            new JingleTransportEstablishedCallback() {
                @Override
                public void onSessionEstablished(BytestreamSession bytestreamSession) {
                    read(bytestreamSession);
                }

                @Override
                public void onSessionFailure(JingleTransportFailureException reason) {
                    //TODO: Send transport failed
                }
            };

    void send(BytestreamSession stream) {
        if (source == null || !source.exists()) {
            throw new IllegalStateException("Source file MUST NOT be null and MUST exist.");
        }

        byte[] filebuf = new byte[(int) source.length()];
        HashElement hashElement = null;
        try {
            hashElement = FileAndHashReader.readAndCalculateHash(source, filebuf, HashManager.ALGORITHM.SHA_256);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read file: " + e, e);
            //TODO: Terminate session.
            return;
        }

        //TODO: session-info with hash

        try {
            stream.getOutputStream().write(filebuf);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Caught Exception while sending file: " + e, e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close OutputStream of ByteStream: " + e, e);
            }
        }

        //TODO: session-info to signalize that file has been sent.
    }

    void read(BytestreamSession session) {
        if (target == null) {
            throw new IllegalStateException("Target file MUST NOT be null.");
        }

        //Become mainstream
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            inputStream = session.getInputStream();
            fileOutputStream = new FileOutputStream(target);

            int size = ((JingleFileTransferChild) proposedContent.getDescription()
                    .getJingleContentDescriptionChildren().get(0)).getSize();

            byte[] filebuf = new byte[size];
            byte[] readbuf = new byte[2048];
            int read = 0;

            while (read < size) {
                int r = inputStream.read(readbuf);

                if (r >= 0) {
                    System.arraycopy(readbuf, 0, filebuf, read, r);
                    read += r;
                } else {
                    //TODO: Terminate?
                }
            }

            fileOutputStream.write(filebuf);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Caught exception while receiving file: " + e, e);
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Caught exception while closing FileOutputStream: " + e, e);
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Caught exception while closing InputStream: " + e, e);
            }
        }
    }

    public JingleManager.FullJidAndSessionId getFullJidAndSessionId() {
        return new JingleManager.FullJidAndSessionId(remote, sessionId);
    }

    @Override
    protected IQ handleSessionInitiate(Jingle sessionInitiate) {
        return state.handleSessionInitiate(sessionInitiate);
    }

    @Override
    protected IQ handleSessionTerminate(Jingle sessionTerminate) {
        return state.handleSessionTerminate(sessionTerminate);
    }

    @Override
    protected IQ handleSessionInfo(Jingle sessionInfo) {
        return state.handleSessionInfo(sessionInfo);
    }

    @Override
    protected IQ handleSessionAccept(Jingle sessionAccept) {
        return state.handleSessionAccept(sessionAccept);
    }

    @Override
    protected IQ handleContentAdd(Jingle contentAdd) {
        return state.handleContentAdd(contentAdd);
    }

    @Override
    protected IQ handleContentAccept(Jingle contentAccept) {
        return state.handleContentAccept(contentAccept);
    }

    @Override
    protected IQ handleContentModify(Jingle contentModify) {
        return state.handleContentModify(contentModify);
    }

    @Override
    protected IQ handleContentReject(Jingle contentReject) {
        return state.handleContentReject(contentReject);
    }

    @Override
    protected IQ handleContentRemove(Jingle contentRemove) {
        return state.handleContentRemove(contentRemove);
    }

    @Override
    protected IQ handleDescriptionInfo(Jingle descriptionInfo) {
        return state.handleDescriptionInfo(descriptionInfo);
    }

    @Override
    protected IQ handleSecurityInfo(Jingle securityInfo) {
        return state.handleSecurityInfo(securityInfo);
    }

    @Override
    protected IQ handleTransportAccept(Jingle transportAccept) {
        return state.handleTransportAccept(transportAccept);
    }

    @Override
    protected IQ handleTransportInfo(Jingle transportInfo) {
        return state.handleTransportInfo(transportInfo);
    }

    @Override
    protected IQ handleTransportReplace(Jingle transportReplace) {
        return state.handleTransportReplace(transportReplace);
    }

    @Override
    protected IQ handleTransportReject(Jingle transportReject) {
        return state.handleTransportReject(transportReject);
    }
}
