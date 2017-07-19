package org.jivesoftware.smackx.jingle3.transport.jingle_s5b;

import org.jivesoftware.smackx.jingle3.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;

/**
 * Created by vanitas on 19.07.17.
 */
public class JingleS5BTransportAdapter implements JingleTransportAdapter<JingleS5BTransport> {

    @Override
    public JingleS5BTransport transportFromElement(JingleContentTransportElement element) {
        JingleS5BTransport transport = new JingleS5BTransport();
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }
}
