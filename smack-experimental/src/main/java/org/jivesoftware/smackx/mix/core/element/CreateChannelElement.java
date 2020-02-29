package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public abstract class CreateChannelElement implements ExtensionElement {

    public static final String ELEMENT = "create";
    public static final String ATTR_CHANNEL = "channel";

    protected final String channel;

    public CreateChannelElement() {
        this(null);
    }

    public CreateChannelElement(String channel) {
        channel = channel != null ? channel.trim() : null;
        this.channel = StringUtils.requireNullOrNotEmpty(channel, "Channel name MUST either be null or NOT empty.");
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .optAttribute(ATTR_CHANNEL, getChannel())
                .closeEmptyElement();
    }

    public static class V1 extends CreateChannelElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1() {
            super();
        }

        public V1(String channel) {
            super(channel);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
