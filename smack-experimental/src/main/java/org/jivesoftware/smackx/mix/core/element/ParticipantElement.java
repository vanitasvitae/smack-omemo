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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.mix.core.MixCoreConstants;

import org.jxmpp.jid.EntityBareJid;

public abstract class ParticipantElement implements ExtensionElement {

    public static final String ELEMENT = "participant";

    protected final NickElement nick;
    protected final JidElement jid;

    public ParticipantElement(NickElement nick, JidElement jid) {
        this.nick = Objects.requireNonNull(nick);
        this.jid = Objects.requireNonNull(jid);
    }

    public NickElement getNick() {
        return nick;
    }

    public JidElement getJid() {
        return jid;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public static class V1 extends ParticipantElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(NickElement nick, JidElement jid) {
            super(nick, jid);
        }

        public V1(String nick, EntityBareJid jid) {
            super(new NickElement(nick), new JidElement(jid));
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            return new XmlStringBuilder(this).rightAngleBracket()
                    .append(getNick())
                    .append(getJid())
                    .closeElement(this);
        }
    }
}
