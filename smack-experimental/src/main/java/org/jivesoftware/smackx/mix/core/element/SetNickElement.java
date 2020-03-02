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

public abstract class SetNickElement implements ExtensionElement {

    public static final String ELEMENT = "setnick";

    private final NickElement nick;

    public SetNickElement(String nick) {
        this(new NickElement(nick));
    }

    public SetNickElement(NickElement nick) {
        this.nick = Objects.requireNonNull(nick);
    }

    public NickElement getNick() {
        return nick;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .append(getNick())
                .closeElement(this);
    }

    public static class V1 extends SetNickElement {

        public static final String NAMESPACE = MixCoreConstants.NAMESPACE_CORE_1;

        public V1(String nick) {
            super(nick);
        }

        public V1(NickElement nick) {
            super(nick);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }
    }
}
