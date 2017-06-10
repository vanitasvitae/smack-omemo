package org.jivesoftware.smackx.jingle_ibb;

import java.lang.ref.WeakReference;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle_ibb.element.JingleIBBTransport;

/**
 * JingleTransportHandler for InBandBytestreams.
 */
public class JingleIBBTransportHandler implements JingleTransportHandler<JingleIBBTransport> {

    private final WeakReference<JingleSessionHandler> jingleSessionHandler;

    public JingleIBBTransportHandler(JingleSessionHandler sessionHandler) {
        this.jingleSessionHandler = new WeakReference<>(sessionHandler);
    }

    @Override
    public void establishOutgoingSession(Jingle request, JingleTransportEstablishedCallback callback) {
        InBandBytestreamSession session;

        try {
            session = InBandBytestreamManager.getByteStreamManager(getConnection())
                    .establishSession(request.getResponder(), request.getSessionId());
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            callback.onSessionFailure(new JingleTransportFailureException(e));
            return;
        }

        callback.onSessionEstablished(session);
    }

    @Override
    public void establishIncomingSession(final Jingle initiate, final JingleTransportEstablishedCallback callback) {
        InBandBytestreamManager.getByteStreamManager(getConnection()).addIncomingBytestreamListener(new BytestreamListener() {
            @Override
            public void incomingBytestreamRequest(BytestreamRequest request) {
                if (request.getFrom().asFullJidIfPossible().equals(initiate.getInitiator())
                        && request.getSessionID().equals(initiate.getSessionId())) {
                    BytestreamSession session;

                    try {
                        session = request.accept();
                    } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                        callback.onSessionFailure(new JingleTransportFailureException(e));
                        return;
                    }
                    callback.onSessionEstablished(session);
                }
            }
        });
    }


    @Override
    public XMPPConnection getConnection() {
        JingleSessionHandler sessionHandler = jingleSessionHandler.get();
        return sessionHandler != null ? sessionHandler.getConnection() : null;
    }
}
