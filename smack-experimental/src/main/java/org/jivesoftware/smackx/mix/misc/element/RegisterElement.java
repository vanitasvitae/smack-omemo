package org.jivesoftware.smackx.mix.misc.element;

import static org.jivesoftware.smackx.mix.misc.MixMiscConstants.NAMESPACE_MISC_0;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.element.NickElement;

public abstract class RegisterElement implements ExtensionElement {

    public static final String ELEMENT = "register";

    private final NickElement nick;

    public RegisterElement(NickElement nickElement) {
        this.nick = Objects.requireNonNull(nickElement, "Nick element MUST NOT be null.");
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(nick)
                .closeElement(this);
    }

    public static class V0 extends RegisterElement {

        public V0(NickElement nickElement) {
            super(nickElement);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE_MISC_0;
        }
    }
}
