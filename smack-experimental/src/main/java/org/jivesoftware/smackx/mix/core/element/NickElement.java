package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class NickElement implements NamedElement {

    public static final String ELEMENT = "nick";

    private final String value;

    public NickElement(String value) {
        value = value != null ? value.trim() : null;
        this.value = StringUtils.requireNotNullNorEmpty(value, "Nickname MUST NOT be null NOR empty.");
    }

    public String getValue() {
        return value;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .escapeAttributeValue(getValue())
                .closeElement(this);
    }
}
