/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.inbox.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.inbox.InboxManager;
import org.jivesoftware.smackx.rsm.packet.RSMSet;

public class InboxElement implements ExtensionElement {

    public static final String ELEMENT = "inbox";
    public static final String ATTR_UNREAD_ONLY = "unread-only";
    public static final String ATTR_MESSAGES = "messages";

    private final RSMSet filter;
    private final boolean unreadOnly;
    private final boolean includeMessages;

    public InboxElement() {
        this(null);
    }

    public InboxElement(RSMSet filter) {
        this(filter, false, true);
    }

    public InboxElement(RSMSet filter, boolean unreadOnly, boolean includeMessages) {
        this.filter = filter;
        this.unreadOnly = unreadOnly;
        this.includeMessages = includeMessages;
    }

    public RSMSet getFilter() {
        return filter;
    }

    public boolean isUnreadOnly() {
        return unreadOnly;
    }

    public boolean isIncludeMessages() {
        return includeMessages;
    }

    @Override
    public String getNamespace() {
        return InboxManager.NAMESPACE_1;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this)
                .optBooleanAttribute(ATTR_UNREAD_ONLY, isUnreadOnly())
                .optBooleanAttributeDefaultTrue(ATTR_MESSAGES, isIncludeMessages());

        if (getFilter() == null) {
            return xml.closeEmptyElement();
        } else {
            return xml.rightAngleBracket()
                    .append(getFilter())
                    .closeElement(this);
        }
    }
}
