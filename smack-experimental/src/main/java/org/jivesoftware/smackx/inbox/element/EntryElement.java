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

import org.jxmpp.jid.EntityBareJid;

public class EntryElement implements ExtensionElement {

    public static final String ELEMENT = "entry";
    public static final String ATTR_UNREAD = "unread";
    public static final String ATTR_JID = "jid";
    public static final String ATTR_ID = "id";

    private final int unreadCount;
    private final EntityBareJid jid;
    private final String id; // TODO: Legacy Stanza ID, Origin ID, Stanza ID or MAM ID?

    public EntryElement(int unreadCount, EntityBareJid jid, String id) {
        this.unreadCount = unreadCount;
        this.jid = jid;
        this.id = id;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public EntityBareJid getJid() {
        return jid;
    }

    public String getId() {
        return id;
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
        return new XmlStringBuilder(this)
                .attribute(ATTR_UNREAD, getUnreadCount())
                .attribute(ATTR_JID, getJid())
                .attribute(ATTR_ID, getId())
                .closeEmptyElement();
    }
}
