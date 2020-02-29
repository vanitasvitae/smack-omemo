package org.jivesoftware.smackx.mix.core.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

public abstract class DestroyChannelElement implements ExtensionElement {

    public static final String ELEMENT = "destroy";
    public static final String ATTR_CHANNEL = "channel";

    private final String channel;

    public DestroyChannelElement(String channel) {
        channel = channel != null ? channel.trim() : null;
        this.channel = StringUtils.requireNotNullNorEmpty(channel, "Channel name MUST NOT be null NOR empty.");
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
        return new XmlStringBuilder(this).attribute(ATTR_CHANNEL, getChannel()).closeEmptyElement();
    }

    public static class V1 extends DestroyChannelElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(String channel) {
            super(channel);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
