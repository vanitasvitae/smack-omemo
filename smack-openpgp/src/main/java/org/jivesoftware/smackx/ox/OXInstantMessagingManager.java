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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.listener.OxMessageListener;
import org.jivesoftware.smackx.ox.listener.internal.SigncryptElementReceivedListener;

import org.jxmpp.jid.BareJid;

/**
 * Entry point of Smacks API for XEP-0374: OpenPGP for XMPP: Instant Messaging.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0374.html">
 *     XEP-0374: OpenPGP for XMPP: Instant Messaging</a>
 */
public final class OXInstantMessagingManager extends Manager implements SigncryptElementReceivedListener {

    public static final String NAMESPACE_0 = "urn:xmpp:openpgp:im:0";

    private static final Map<XMPPConnection, OXInstantMessagingManager> INSTANCES = new WeakHashMap<>();

    private final Set<OxMessageListener> oxMessageListeners = new HashSet<>();

    private OXInstantMessagingManager(final XMPPConnection connection) {
        super(connection);
        OpenPgpManager.getInstanceFor(connection).registerSigncryptReceivedListener(this);
        announceSupportForOxInstantMessaging();
    }

    public static OXInstantMessagingManager getInstanceFor(XMPPConnection connection) {
        OXInstantMessagingManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new OXInstantMessagingManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public void announceSupportForOxInstantMessaging() {
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(NAMESPACE_0);
    }

    /**
     * Determine, whether a contact announces support for XEP-0374: OpenPGP for XMPP: Instant Messaging.
     *
     * @param jid {@link BareJid} of the contact in question.
     * @return true if contact announces support, otherwise false.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean contactSupportsOxInstantMessaging(BareJid jid)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid, NAMESPACE_0);
    }

    /**
     * Determine, whether a contact announces support for XEP-0374: OpenPGP for XMPP: Instant Messaging.
     *
     * @param contact {@link OpenPgpContact} in question.
     * @return true if contact announces support, otherwise false.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean contactSupportsOxInstantMessaging(OpenPgpContact contact)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return contactSupportsOxInstantMessaging(contact.getJid());
    }

    public boolean addOxMessageListener(OxMessageListener listener) {
        return oxMessageListeners.add(listener);
    }

    public boolean removeOxMessageListener(OxMessageListener listener) {
        return oxMessageListeners.remove(listener);
    }

    @Override
    public void signcryptElementReceived(OpenPgpContact contact, Message originalMessage, SigncryptElement signcryptElement) {
        for (OxMessageListener listener : oxMessageListeners) {
            listener.newIncomingOxMessage(contact, originalMessage, signcryptElement);
        }
    }

    public void sendOxMessage(OpenPgpContact contact, CharSequence body)
            throws InterruptedException, MissingOpenPgpKeyPairException, IOException,
            SmackException.NotConnectedException, SmackOpenPgpException, SmackException.NotLoggedInException {
        Message message = new Message(contact.getJid());
        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body(null, body.toString()));

        // Add additional information to the message
        message.addExtension(new ExplicitMessageEncryptionElement(
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        StoreHint.set(message);
        message.setBody("This message is encrypted using XEP-0374: OpenPGP for XMPP: Instant Messaging.");

        contact.addSignedEncryptedPayloadTo(message, payload);

        ChatManager.getInstanceFor(connection()).chatWith(contact.getJid().asEntityBareJidIfPossible()).send(message);
    }
}
