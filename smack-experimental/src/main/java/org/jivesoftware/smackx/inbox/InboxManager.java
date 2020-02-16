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
package org.jivesoftware.smackx.inbox;

import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

public final class InboxManager extends Manager {

    public static final String NAMESPACE_1 = "urn:xmpp:inbox:1";
    private static final Map<XMPPConnection, InboxManager> INSTANCES = new WeakHashMap<>();

    private InboxManager(XMPPConnection connection) {
        super(connection);
    }

    public static InboxManager getInstanceFor(XMPPConnection connection) {
        InboxManager inboxManager = INSTANCES.get(connection);
        if (inboxManager == null) {
            inboxManager = new InboxManager(connection);
            INSTANCES.put(connection, inboxManager);
        }
        return inboxManager;
    }
}
