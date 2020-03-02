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
