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
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ciphers.Aes256GcmNoPadding;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jet.provider.JetSecurityProvider;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.util.Role;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle Encrypted Transfers (XEP-XXXX).
 */
public final class JetManager extends Manager implements JingleDescriptionManager {

    private static final Logger LOGGER = Logger.getLogger(JetManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JetManager> INSTANCES = new WeakHashMap<>();
    private static final HashMap<String, JingleEncryptionMethod> encryptionMethods = new HashMap<>();
    private static final HashMap<String, ExtensionElementProvider<?>> encryptionMethodProviders = new HashMap<>();

    private final JingleManager jingleManager;

    static {
        JingleManager.addJingleSecurityAdapter(new JetSecurityAdapter());
        JingleManager.addJingleSecurityProvider(new JetSecurityProvider());
    }

    private JetManager(XMPPConnection connection) {
        super(connection);
        this.jingleManager = JingleManager.getInstanceFor(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
        jingleManager.addJingleDescriptionManager(this);
    }

    public static JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public OutgoingFileOfferController sendEncryptedFile(FullJid recipient, File file, JingleEncryptionMethod method) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        ServiceDiscoveryManager disco = ServiceDiscoveryManager.getInstanceFor(connection());
        if (!disco.supportsFeature(recipient, getNamespace()) || !disco.supportsFeature(recipient, method.getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), recipient);
        }

        JingleSession session = jingleManager.createSession(Role.initiator, recipient);

        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(content);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file);
        content.setDescription(offer);

        JingleTransportManager transportManager = jingleManager.getBestAvailableTransportManager(recipient);
        content.setTransport(transportManager.createTransportForInitiator(content));

        JetSecurity security = new JetSecurity(method, recipient, content.getName(), Aes256GcmNoPadding.NAMESPACE);
        content.setSecurity(security);
        session.sendInitiate(connection());

        return offer;
    }

    public void registerEncryptionMethod(JingleEncryptionMethod method) {
        encryptionMethods.put(method.getNamespace(), method);
    }

    public void unregisterEncryptionMethod(String namespace) {
        encryptionMethods.remove(namespace);
    }

    public JingleEncryptionMethod getEncryptionMethod(String namespace) {
        return encryptionMethods.get(namespace);
    }

    public static void registerEncryptionMethodProvider(String namespace, ExtensionElementProvider<?> provider) {
        encryptionMethodProviders.put(namespace, provider);
    }

    public static void removeEncryptionMethodProvider(String namespace) {
        encryptionMethodProviders.remove(namespace);
    }

    public static ExtensionElementProvider<?> getEncryptionMethodProvider(String namespace) {
        return encryptionMethodProviders.get(namespace);
    }

    @Override
    public String getNamespace() {
        return JetSecurity.NAMESPACE;
    }

    @Override
    public void notifySessionInitiate(JingleSession session) {
        JingleFileTransferManager.getInstanceFor(connection()).notifySessionInitiate(session);
    }

    @Override
    public void notifyContentAdd(JingleSession session, JingleContent content) {
        JingleFileTransferManager.getInstanceFor(connection()).notifyContentAdd(session, content);
    }
}
