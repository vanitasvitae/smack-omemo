package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public abstract class SetNickElement implements ExtensionElement {

    public static final String ELEMENT = "setnick";

    private final NickElement nick;

    public SetNickElement(String nick) {
        this(new NickElement(nick));
    }

    public SetNickElement(NickElement nick) {
        this.nick = Objects.requireNonNull(nick);
    }

    public NickElement getNick() {
        return nick;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .append(getNick())
                .closeElement(this);
    }

    public static class V1 extends SetNickElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(String nick) {
            super(nick);
        }

        public V1(NickElement nick) {
            super(nick);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
