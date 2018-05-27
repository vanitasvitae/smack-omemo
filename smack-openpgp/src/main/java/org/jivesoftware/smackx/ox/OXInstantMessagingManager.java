/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;

import org.jxmpp.jid.BareJid;

public class OXInstantMessagingManager extends Manager {

    private static final Map<XMPPConnection, OXInstantMessagingManager> INSTANCES = new WeakHashMap<>();
    private final OpenPgpManager openPgpManager;

    private OXInstantMessagingManager(XMPPConnection connection) {
        super(connection);
        this.openPgpManager = OpenPgpManager.getInstanceFor(connection);
    }

    public static OXInstantMessagingManager getInstanceFor(XMPPConnection connection) {
        OXInstantMessagingManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OXInstantMessagingManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void send(List<ExtensionElement> messageContent, BareJid recipient) {

    }
}
