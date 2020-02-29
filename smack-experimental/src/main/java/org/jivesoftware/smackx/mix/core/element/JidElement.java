package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.EntityBareJid;

public class JidElement implements NamedElement {

    public static final String ELEMENT = "jid";

    private final EntityBareJid value;

    public JidElement(EntityBareJid value) {
        this.value = Objects.requireNonNull(value);
    }

    public EntityBareJid getValue() {
        return value;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .escapeAttributeValue(getValue().asEntityBareJidString())
                .closeElement(this);
    }
}
