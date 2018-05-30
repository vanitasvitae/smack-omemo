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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

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

    private OXInstantMessagingManager(XMPPConnection connection) {
        super(connection);
        this.openPgpManager = OpenPgpManager.getInstanceFor(connection);
        this.chatManager = ChatManager.getInstanceFor(connection);
    }

    public static OXInstantMessagingManager getInstanceFor(XMPPConnection connection) {
        OXInstantMessagingManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OXInstantMessagingManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
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
     * @throws SmackException.FeatureNotSupportedException if the contact does not announce support for XEP-0374.
     */
    public OpenPgpEncryptedChat chatWith(EntityBareJid jid)
            throws SmackOpenPgpException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException,
            SmackException.FeatureNotSupportedException {
        if (!contactSupportsOxInstantMessaging(jid)) {
            throw new SmackException.FeatureNotSupportedException(NAMESPACE_0, jid);
        }

        OpenPgpFingerprints theirKeys = openPgpManager.determineContactsKeys(jid);
        OpenPgpFingerprints ourKeys = openPgpManager.determineContactsKeys(connection().getUser().asBareJid());
        Chat chat = chatManager.chatWith(jid);
        return new OpenPgpEncryptedChat(openPgpManager.getOpenPgpProvider(), chat, ourKeys, theirKeys);
    }

    public void addOpenPgpEncryptedMessageListener() {

    }
}
