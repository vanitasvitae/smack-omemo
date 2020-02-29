package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public abstract class LeaveElement implements ExtensionElement {

    public static final String ELEMENT = "leave";

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).closeEmptyElement();
    }

    public static class V1 extends LeaveElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
