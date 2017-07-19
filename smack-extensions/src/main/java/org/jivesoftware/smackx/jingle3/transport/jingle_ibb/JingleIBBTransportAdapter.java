package org.jivesoftware.smackx.jingle3.transport.jingle_ibb;

import org.jivesoftware.smackx.jingle3.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.transport.jingle_ibb.element.JingleIBBTransportElement;

/**
 * Created by vanitas on 18.07.17.
 */
public class JingleIBBTransportAdapter implements JingleTransportAdapter<JingleIBBTransport> {
    @Override
    public JingleIBBTransport transportFromElement(JingleContentTransportElement element) {
        JingleIBBTransportElement transport = (JingleIBBTransportElement) element;
        return new JingleIBBTransport(transport.getSid(), transport.getBlockSize());
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE;
    }
}
