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

public abstract class JoinElement
        extends AbstractSubscriptionsModifyingElement
        implements ExtensionElement {

    public static final String ELEMENT = "join";
    public static final String ATTR_ID = "id";

    private final NickElement nick;
    private final String id;

    public JoinElement(List<SubscribeElement> nodeSubscriptions) {
        this(nodeSubscriptions, null);
    }

    public JoinElement(List<SubscribeElement> nodeSubscriptions, NickElement nick) {
        this(null, nodeSubscriptions, nick);
    }

    public JoinElement(String id, List<SubscribeElement> nodeSubscriptions, NickElement nick) {
        super(nodeSubscriptions);
        this.id = id;
        this.nick = nick;
    }

    /**
     * Users Stable Participant ID. May be null.
     * @return id
     */
    public String getId() {
        return id;
    }

    public NickElement getNick() {
        return nick;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public static class V1 extends JoinElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(List<SubscribeElement> nodeSubscriptions) {
            super(nodeSubscriptions);
        }

        public V1(List<SubscribeElement> nodeSubscriptions, NickElement nick) {
            super(nodeSubscriptions, nick);
        }

        public V1(String id, List<SubscribeElement> nodeSubscriptions, NickElement nick) {
            super(id, nodeSubscriptions, nick);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this)
                    .optAttribute(ATTR_ID, getId())
                    .rightAngleBracket();
            appendSubscribeElementsToXml(xml);
            xml.optAppend(getNick());
            return xml.closeElement(this);
        }
    }

}
