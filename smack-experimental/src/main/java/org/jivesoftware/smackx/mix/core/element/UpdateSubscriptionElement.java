/**
 *
 * Copyright 2020 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
