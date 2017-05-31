/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_ibb;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_ibb.provider.JingleInBandByteStreamTransportProvider;

import java.util.Random;
import java.util.WeakHashMap;

/**
 * Manager for Jingle In-Band-ByteStreams.
 */
public final class JingleInBandByteStreamManager extends Manager {

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";

    private static final WeakHashMap<XMPPConnection, JingleInBandByteStreamManager> INSTANCES = new WeakHashMap<>();

    private JingleInBandByteStreamManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentTransportProvider(NAMESPACE_V1, new JingleInBandByteStreamTransportProvider());
    }

    public static JingleInBandByteStreamManager getInstanceFor(XMPPConnection connection) {
        JingleInBandByteStreamManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleInBandByteStreamManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Generate a random session id.
     * @return
     */
    public static String generateSessionId() {
        return Integer.toString(64,new Random().nextInt());
    }
}
