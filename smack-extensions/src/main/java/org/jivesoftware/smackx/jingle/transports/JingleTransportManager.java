package org.jivesoftware.smackx.jingle.transports;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;

/**
 * Created by vanitas on 19.06.17.
 */
public abstract class JingleTransportManager<D extends JingleContentTransport> {

    public abstract String getNamespace();

    public abstract D createTransport();

    public abstract D createTransport(Jingle request);

}
