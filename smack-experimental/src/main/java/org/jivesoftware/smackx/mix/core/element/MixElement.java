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
