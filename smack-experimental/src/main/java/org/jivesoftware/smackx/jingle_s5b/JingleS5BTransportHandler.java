package org.jivesoftware.smackx.jingle_s5b;

import java.lang.ref.WeakReference;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;

/**
 * JingleTransportHandler for Socks5Bytestreams.
 */
public class JingleS5BTransportHandler implements JingleTransportHandler<JingleS5BTransport> {

    private final WeakReference<JingleSessionHandler> sessionHandler;

    public JingleS5BTransportHandler(JingleSessionHandler sessionHandler) {
        this.sessionHandler = new WeakReference<>(sessionHandler);
    }

    @Override
    public void establishOutgoingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport transport, JingleTransportEstablishedCallback callback) {

    }

    @Override
    public void establishIncomingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport transport, JingleTransportEstablishedCallback callback) {

    }

    @Override
    public XMPPConnection getConnection() {
        JingleSessionHandler handler = sessionHandler.get();
        return handler != null ? handler.getConnection() : null;
    }
}
