package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Transport<D extends JingleContentTransportElement> {

    public abstract D getElement();

    public abstract String getNamespace();

    public abstract BytestreamSession establishIncomingBytestreamSession(BytestreamSessionEstablishedListener listener);

    public abstract BytestreamSession establishOutgoingBytestreamSession(BytestreamSessionEstablishedListener listener);

    public abstract JingleElement handleTransportInfo(JingleContentTransportInfoElement info);
}
