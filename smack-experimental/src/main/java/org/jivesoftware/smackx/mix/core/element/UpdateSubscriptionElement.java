package org.jivesoftware.smackx.mix.core.element;

import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

import org.jxmpp.jid.EntityBareJid;

public abstract class UpdateSubscriptionElement
        extends AbstractSubscriptionsModifyingElement
        implements ExtensionElement {

    public static final String ELEMENT = "update-subscription";
    public static final String ATTR_JID = "jid";

    private final EntityBareJid jid;

    public UpdateSubscriptionElement(List<SubscribeElement> nodeSubscriptions, EntityBareJid jid) {
        super(nodeSubscriptions);
        this.jid = jid;
    }

    /**
     * Possibly null!
     * @return jid or null
     */
    public EntityBareJid getJid() {
        return jid;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public static class V1 extends UpdateSubscriptionElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(List<SubscribeElement> nodeSubscriptions) {
            super(nodeSubscriptions, null);
        }

        public V1(List<SubscribeElement> nodeSubscriptions, EntityBareJid jid) {
            super(nodeSubscriptions, jid);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this)
                    .optAttribute(ATTR_JID, getJid())
                    .rightAngleBracket();
            appendSubscribeElementsToXml(xml);
            return xml.closeElement(this);
        }
    }
}
