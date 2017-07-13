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
package org.jivesoftware.smackx.jet;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

/**
 * Created by vanitas on 13.07.17.
 */
public final class JingleEncryptedTransferManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:jingle:jet:0";

    private static final WeakHashMap<XMPPConnection, JingleEncryptedTransferManager> INSTANCES = new WeakHashMap<>();

    private static final Map<String, JingleEncryptionMethod> encryptionProviders = new HashMap<>();

    private JingleEncryptedTransferManager(XMPPConnection connection) {
        super(connection);
    }

    public static JingleEncryptedTransferManager getInstanceFor(XMPPConnection connection) {
        JingleEncryptedTransferManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleEncryptedTransferManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }


    public void registerEncryptionProvider(String namespace, JingleEncryptionMethod provider) {
        encryptionProviders.put(namespace, provider);
    }

    public void unregisterEncryptionProvider(String namespace) {
        encryptionProviders.remove(namespace);
    }

    public JingleEncryptionMethod getEncryptionProvider(String namespace) {
        return encryptionProviders.get(namespace);
    }
}
