package org.jivesoftware.smackx.jingle_encrypted_transfer.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle_encrypted_transfer.JingleEncryptedTransferManager;

/**
 * Created by vanitas on 13.07.17.
 */
public class SecurityElement implements ExtensionElement {
    public static final String ELEMENT = "security";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TYPE = "type";

    private final ExtensionElement child;
    private final String name;

    public SecurityElement(String name, ExtensionElement child) {
        this.name = name;
        this.child = child;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_NAME, name).attribute(ATTR_TYPE, child.getNamespace());
        xml.rightAngleBracket();
        xml.element(child);
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getNamespace() {
        return JingleEncryptedTransferManager.NAMESPACE;
    }
}
