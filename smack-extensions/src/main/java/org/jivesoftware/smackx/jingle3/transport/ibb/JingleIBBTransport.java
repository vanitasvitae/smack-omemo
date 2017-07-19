package org.jivesoftware.smackx.jingle3.transport.ibb;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle3.internal.Session;
import org.jivesoftware.smackx.jingle3.internal.Transport;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;
import org.jivesoftware.smackx.jingle3.transport.jingle_ibb.element.JingleIBBTransportElement;

/**
 * Created by vanitas on 18.07.17.
 */
public class JingleIBBTransport extends Transport<JingleIBBTransportElement> {

    private final String streamId;
    private final Short blockSize;

    public JingleIBBTransport(String streamId, Short blockSize) {
        this.streamId = streamId;
        this.blockSize = blockSize;
    }

    public JingleIBBTransport() {
        this(StringUtils.randomString(10), JingleIBBTransportElement.DEFAULT_BLOCK_SIZE);
    }

    @Override
    public JingleIBBTransportElement getElement() {
        return new JingleIBBTransportElement(blockSize, streamId);
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransportElement.NAMESPACE_V1;
    }

    @Override
    public BytestreamSession establishIncomingBytestreamSession(final Session session) {

        InBandBytestreamManager.getByteStreamManager(session.getJingleManager().getConnection())
                .addIncomingBytestreamListener(new BytestreamListener() {
                    @Override
                    public void incomingBytestreamRequest(BytestreamRequest request) {
                        if (request.getFrom().asFullJidIfPossible().equals(session.getPeer())
                                && request.getSessionID().equals(theirProposal.getSessionId())) {
                            BytestreamSession bytestreamSession;

                            try {
                                bytestreamSession = request.accept();
                            } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                        .onException(e);
                                return;
                            }
                            callback.onSessionInitiated(bytestreamSession);
                        }
                    }
                });
    }

    @Override
    public BytestreamSession establishOutgoingBytestreamSession(BytestreamSessionEstablishedListener listener) {
        return null;
    }
}
