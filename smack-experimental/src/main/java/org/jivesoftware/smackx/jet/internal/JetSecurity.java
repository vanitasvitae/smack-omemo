package org.jivesoftware.smackx.jet.internal;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.internal.JingleSecurity;

/**
 * Created by vanitas on 22.07.17.
 */
public class JetSecurity extends JingleSecurity<JetSecurityElement> {

    public static final String NAMESPACE_V0 = "urn:xmpp:jingle:jet:0";
    public static final String NAMESPACE = NAMESPACE_V0;

    private ExtensionElement child;

    @Override
    public JetSecurityElement getElement() {
        return new JetSecurityElement(getParent().getName(), child);
    }

    @Override
    public JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element, JingleElement wrapping) {
        return null;
    }
}
