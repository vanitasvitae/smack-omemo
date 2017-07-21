package org.jivesoftware.smackx.jft.internal;

import org.jivesoftware.smackx.jingle.internal.JingleDescription;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;

/**
 * Created by vanitas on 22.07.17.
 */
public abstract class JingleFileTransfer extends JingleDescription<JingleFileTransferElement> {

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";
    public static final String NAMESPACE = NAMESPACE_V5;

    @Override
    public JingleFileTransferElement getElement() {
        return null;
    }
}
