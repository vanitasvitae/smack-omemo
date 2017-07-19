package org.jivesoftware.smackx.jingle3.transport.jingle_ibb;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle3.internal.Transport;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;
import org.jivesoftware.smackx.jingle3.transport.jingle_ibb.element.JingleIBBTransportElement;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 18.07.17.
 */
public class JingleIBBTransport extends Transport<JingleIBBTransportElement> {

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    private final String streamId;
    private final Short blockSize;

    private JingleIBBTransport peersProposal;

    public JingleIBBTransport(String streamId, Short blockSize) {
        this.streamId = streamId;
        this.blockSize = blockSize;
    }

    public JingleIBBTransport() {
        this(StringUtils.randomString(10), JingleIBBTransportElement.DEFAULT_BLOCK_SIZE);
    }

    @Override
    public JingleIBBTransportElement getElement() {
        return new JingleIBBTransportElement(streamId, blockSize);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void establishIncomingBytestreamSession(final FullJid peer, final String transportSessionId, final BytestreamSessionEstablishedListener listener, XMPPConnection connection) {
        InBandBytestreamManager.getByteStreamManager(connection)
                .addIncomingBytestreamListener(new BytestreamListener() {
                    @Override
                    public void incomingBytestreamRequest(BytestreamRequest request) {
                        if (request.getFrom().asFullJidIfPossible().equals(peer)
                                && request.getSessionID().equals(transportSessionId)) {
                            BytestreamSession bytestreamSession;

                            try {
                                bytestreamSession = request.accept();
                            } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                                listener.onBytestreamSessionFailed(e);
                                return;
                            }
                            listener.onBytestreamSessionEstablished(bytestreamSession);
                        }
                    }
                });
    }

    @Override
    public void establishOutgoingBytestreamSession(FullJid peer, String transportSessionId, BytestreamSessionEstablishedListener listener, XMPPConnection connection) {
        InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        inBandBytestreamManager.setDefaultBlockSize(blockSize);
        try {
            BytestreamSession session = inBandBytestreamManager.establishSession(peer, transportSessionId);
            listener.onBytestreamSessionEstablished(session);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            listener.onBytestreamSessionFailed(e);
        }
    }

    @Override
    public void setPeersProposal(Transport<?> peersProposal) {
        this.peersProposal = (JingleIBBTransport) peersProposal;
    }

    @Override
    public void handleTransportInfo(JingleContentTransportInfoElement info) {
        // Nothing to do.
    }
}
