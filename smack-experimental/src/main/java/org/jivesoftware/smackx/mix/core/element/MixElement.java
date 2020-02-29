package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public abstract class MixElement implements ExtensionElement {

    public static final String ELEMENT = "mix";

    protected final NickElement nick;
    protected final JidElement jid;

    public MixElement(NickElement nick) {
        this(nick, null);
    }

    public MixElement(NickElement nick, JidElement jid) {
        this.nick = nick;
        this.jid = jid;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .append(nick)
                .optAppend(jid)
                .closeElement(this);
    }

    public static class V1 extends MixElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(NickElement nick) {
            super(nick);
        }

        public V1(NickElement nick, JidElement jid) {
            super(nick, jid);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
