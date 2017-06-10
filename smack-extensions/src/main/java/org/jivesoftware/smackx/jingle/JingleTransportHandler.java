package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Handler for JingleTransports
 */
public interface JingleTransportHandler<D extends JingleContentTransport> {

    void establishOutgoingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport transport, JingleTransportEstablishedCallback callback);

    void establishIncomingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport transport, JingleTransportEstablishedCallback callback);

    XMPPConnection getConnection();
}
