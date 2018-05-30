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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import org.jxmpp.jid.Jid;

public class OpenPgpEncryptedChat {

    private final Chat chat;
    private final OpenPgpFingerprints contactsFingerprints;
    private final OpenPgpFingerprints ourFingerprints;
    private final OpenPgpProvider cryptoProvider;
    private final OpenPgpV4Fingerprint singingKey;

    public OpenPgpEncryptedChat(OpenPgpProvider cryptoProvider,
                                Chat chat,
                                OpenPgpFingerprints ourFingerprints,
                                OpenPgpFingerprints contactsFingerprints) {
        this.cryptoProvider = cryptoProvider;
        this.chat = chat;
        this.singingKey = cryptoProvider.primaryOpenPgpKeyPairFingerprint();
        this.ourFingerprints = ourFingerprints;
        this.contactsFingerprints = contactsFingerprints;
    }

    public void send(Message message, List<ExtensionElement> payload)
            throws MissingOpenPgpKeyPairException {
        Set<OpenPgpV4Fingerprint> encryptionFingerprints = new HashSet<>(contactsFingerprints.getActiveKeys());
        encryptionFingerprints.addAll(ourFingerprints.getActiveKeys());

        SigncryptElement preparedPayload = new SigncryptElement(
                Collections.<Jid>singleton(chat.getXmppAddressOfChatPartner()),
                payload);
        OpenPgpElement encryptedPayload;

        // Encrypt the payload
        try {
            encryptedPayload = cryptoProvider.signAndEncrypt(
                    preparedPayload,
                    singingKey,
                    encryptionFingerprints);
        } catch (MissingOpenPgpPublicKeyException e) {
            throw new AssertionError("Missing OpenPGP public key, even though this should not happen here.", e);
        }

        // Add encrypted payload to message
        message.addExtension(encryptedPayload);

        // Add additional information to the message
        message.addExtension(new ExplicitMessageEncryptionElement(
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        StoreHint.set(message);
        message.setBody("This message is encrypted using XEP-0374: OpenPGP for XMPP: Instant Messaging.");
    }

    public void send(Message message, CharSequence body)
            throws MissingOpenPgpKeyPairException {
        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body(null, body.toString()));
        send(message, payload);
    }
}
