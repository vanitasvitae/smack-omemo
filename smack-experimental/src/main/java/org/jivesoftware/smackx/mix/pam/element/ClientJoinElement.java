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
package org.jivesoftware.smackx.mix.pam.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.element.JoinElement;
import org.jivesoftware.smackx.mix.pam.MixPamConstants;

import org.jxmpp.jid.EntityBareJid;

public abstract class ClientJoinElement implements ExtensionElement {

    public static final String ELEMENT = "client-join";
    public static final String ATTR_CHANNEL = "channel";

    private final EntityBareJid channel;
    private final JoinElement join;

    public ClientJoinElement(EntityBareJid channel, JoinElement join) {
        this.channel = Objects.requireNonNull(channel);
        this.join = Objects.requireNonNull(join);
    }

    public EntityBareJid getChannel() {
        return channel;
    }

    public JoinElement getJoin() {
        return join;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_CHANNEL, getChannel())
                .rightAngleBracket()
                .append(getJoin())
                .closeElement(this);
    }

    public static class V2 extends ClientJoinElement {

        public static final String NAMESPACE = MixPamConstants.NAMESPACE_PAM_2;

        public V2(EntityBareJid channel, JoinElement.V1 join) {
            super(channel, join);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
