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

public class FinElement implements ExtensionElement {

    public static final String ELEMENT = "fin";
    public static final String ATTR_TOTAL = "total";
    public static final String ATTR_UNREAD = "unread";
    public static final String ATTR_ALL_UNREAD = "all-unread";

    private final int total;
    private final int unread;
    private final int allUnread;

    private RSMSet rsmData; // TODO: What is RSM data? RSM set?

    public FinElement(RSMSet rsmData, int total, int unread, int allUnread) {
        this.rsmData = rsmData;
        this.total = total;
        this.unread = unread;
        this.allUnread = allUnread;
    }

    public RSMSet getRsmData() {
        return rsmData;
    }

    public int getTotal() {
        return total;
    }

    public int getUnread() {
        return unread;
    }

    public int getAllUnread() {
        return allUnread;
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
                .attribute(ATTR_TOTAL, getTotal())
                .attribute(ATTR_UNREAD, getUnread())
                .attribute(ATTR_ALL_UNREAD, getAllUnread())
                .rightAngleBracket()
                .optAppend(getRsmData())
                .closeElement(this);
    }
}
