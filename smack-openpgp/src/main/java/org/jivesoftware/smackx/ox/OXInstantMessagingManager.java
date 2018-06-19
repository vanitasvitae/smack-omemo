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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.chat.OpenPgpEncryptedChat;
import org.jivesoftware.smackx.ox.chat.OpenPgpFingerprints;
import org.jivesoftware.smackx.ox.chat.OpenPgpMessage;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.listener.OpenPgpEncryptedMessageListener;
import org.jivesoftware.smackx.ox.util.DecryptedBytesAndMetadata;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Entry point of Smacks API for XEP-0374: OpenPGP for XMPP: Instant Messaging.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0374.html">
 *     XEP-0374: OpenPGP for XMPP: Instant Messaging</a>
 */
public final class OXInstantMessagingManager extends Manager {

    public static final String NAMESPACE_0 = "urn:xmpp:openpgp:im:0";

    private static final Logger LOGGER = Logger.getLogger(OXInstantMessagingManager.class.getName());

    private static final Map<XMPPConnection, OXInstantMessagingManager> INSTANCES = new WeakHashMap<>();
    private final OpenPgpManager openPgpManager;
    private final ChatManager chatManager;

    private final Set<OpenPgpEncryptedMessageListener> chatMessageListeners = new HashSet<>();
    private final Map<BareJid, OpenPgpEncryptedChat> chats = new HashMap<>();

    private OXInstantMessagingManager(final XMPPConnection connection) {
        super(connection);
        this.openPgpManager = OpenPgpManager.getInstanceFor(connection);
        this.chatManager = ChatManager.getInstanceFor(connection);
        chatManager.addIncomingListener(incomingChatMessageListener);
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
     * Start an encrypted chat with {@code jid}.
     * The chat is encrypted with OpenPGP for XMPP: Instant Messaging (XEP-0374).
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html">XEP-0374: OpenPGP for XMPP: Instant Messaging</a>
     * @param jid {@link BareJid} of the contact.
     * @return {@link OpenPgpEncryptedChat} with the contact.
     * @throws SmackOpenPgpException if something happens while gathering fingerprints.
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public OpenPgpEncryptedChat chatWith(EntityBareJid jid)
            throws SmackOpenPgpException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        OpenPgpFingerprints theirKeys = openPgpManager.determineContactsKeys(jid);
        OpenPgpFingerprints ourKeys = openPgpManager.determineContactsKeys(connection().getUser().asBareJid());
        Chat chat = chatManager.chatWith(jid);
        return new OpenPgpEncryptedChat(openPgpManager.getOpenPgpProvider(), chat, ourKeys, theirKeys);
    }

    public boolean addOpenPgpEncryptedMessageListener(OpenPgpEncryptedMessageListener listener) {
        return chatMessageListeners.add(listener);
    }

    public boolean removeOpenPgpEncryptedMessageListener(OpenPgpEncryptedMessageListener listener) {
        return chatMessageListeners.remove(listener);
    }

    private final IncomingChatMessageListener incomingChatMessageListener =
            new IncomingChatMessageListener() {
                @Override
                public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                    OpenPgpElement element = message.getExtension(OpenPgpElement.ELEMENT, OpenPgpElement.NAMESPACE);
                    if (element == null) {
                        return;
                    }

                    OpenPgpProvider provider = openPgpManager.getOpenPgpProvider();
                    byte[] decoded = Base64.decode(element.getEncryptedBase64MessageContent());

                    try {
                        OpenPgpEncryptedChat encryptedChat = chatWith(from);
                        DecryptedBytesAndMetadata decryptedBytes = provider.decrypt(decoded, from.asBareJid(), null);

                        OpenPgpMessage openPgpMessage = new OpenPgpMessage(decryptedBytes.getBytes(),
                                new OpenPgpMessage.Metadata(decryptedBytes.getDecryptionKey(),
                                        decryptedBytes.getVerifiedSignatures()));

                        OpenPgpContentElement contentElement = openPgpMessage.getOpenPgpContentElement();
                        if (openPgpMessage.getState() != OpenPgpMessage.State.signcrypt) {
                            LOGGER.log(Level.WARNING, "Decrypted content is not a signcrypt element. Ignore it.");
                            return;
                        }

                        SigncryptElement signcryptElement = (SigncryptElement) contentElement;
                        for (OpenPgpEncryptedMessageListener l : chatMessageListeners) {
                            l.newIncomingOxMessage(from, message, signcryptElement, encryptedChat);
                        }
                    } catch (SmackOpenPgpException e) {
                        LOGGER.log(Level.WARNING, "Could not start chat with " + from, e);
                    } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                        LOGGER.log(Level.WARNING, "Something went wrong.", e);
                    } catch (MissingOpenPgpKeyPairException e) {
                        LOGGER.log(Level.WARNING, "Could not decrypt message " + message.getStanzaId() + ": Missing secret key", e);
                    } catch (XmlPullParserException | IOException e) {
                        LOGGER.log(Level.WARNING, "Could not parse decrypted content element", e);
                    }

                }
            };
}
