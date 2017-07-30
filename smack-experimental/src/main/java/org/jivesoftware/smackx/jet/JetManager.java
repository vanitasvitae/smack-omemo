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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jet.internal.JetSecurity;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.internal.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.components.JingleContent;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.util.Role;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle Encrypted Transfers (XEP-XXXX).
 */
public final class JetManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JetManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JetManager> INSTANCES = new WeakHashMap<>();

    private static final Map<String, JingleEncryptionMethod> encryptionMethods = new HashMap<>();

    private final JingleManager jingleManager;

    private JetManager(XMPPConnection connection) {
        super(connection);
        this.jingleManager = JingleManager.getInstanceFor(connection);
    }

    public static JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public OutgoingFileOfferController sendEncryptedFile(FullJid recipient, File file, String encryptionMethodNamespace) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        JingleSession session = jingleManager.createSession(Role.initiator, recipient);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file);
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager();
        content.setTransport(transportManager.createTransport(content));

        JetSecurity security = new JetSecurity(encryptionMethodNamespace, connection());
        content.setSecurity(security);
        session.initiate(connection());

        return offer;
    }


    public void registerEncryptionMethod(String namespace, JingleEncryptionMethod method) {
        encryptionMethods.put(namespace, method);
    }

    public void unregisterEncryptionMethod(String namespace) {
        encryptionMethods.remove(namespace);
    }

    public JingleEncryptionMethod getEncryptionMethod(String namespace) {
        return encryptionMethods.get(namespace);
    }

}
