package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public class SubscribeElement implements NamedElement {

    public static final String ELEMENT = "subscribe";
    public static final String ATTR_NODE = "node";

    private final String node;

    public SubscribeElement(String node) {
        this.node = node;
    }

    public String getValue() {
        return node;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_NODE, getValue())
                .closeEmptyElement();
    }

}
