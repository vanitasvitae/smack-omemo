package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Handler for JingleTransports
 */
public interface JingleTransportHandler<D extends JingleContentTransport> {

    void establishOutgoingSession(Jingle request, JingleTransportEstablishedCallback callback);

    void establishIncomingSession(Jingle request, JingleTransportEstablishedCallback callback);

    XMPPConnection getConnection();
}
