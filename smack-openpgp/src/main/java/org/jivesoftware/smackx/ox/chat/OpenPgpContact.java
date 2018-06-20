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
package org.jivesoftware.smackx.ox.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.DecryptedBytesAndMetadata;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParserException;

public class OpenPgpContact {

    private final Chat chat;
    private final OpenPgpFingerprints contactsFingerprints;
    private final OpenPgpFingerprints ourFingerprints;
    private final OpenPgpProvider cryptoProvider;
    private final OpenPgpV4Fingerprint singingKey;

    public OpenPgpContact(OpenPgpProvider cryptoProvider,
                          Chat chat,
                          OpenPgpFingerprints ourFingerprints,
                          OpenPgpFingerprints contactsFingerprints) {
        this.cryptoProvider = cryptoProvider;
        this.chat = chat;
        this.singingKey = cryptoProvider.getStore().getPrimaryOpenPgpKeyPairFingerprint();
        this.ourFingerprints = ourFingerprints;
        this.contactsFingerprints = contactsFingerprints;
    }

    public EntityBareJid getJidOfChatPartner() {
        return chat.getXmppAddressOfChatPartner();
    }

    public OpenPgpFingerprints getContactsFingerprints() {
        return contactsFingerprints;
    }

    public void send(Message message, List<ExtensionElement> payload)
            throws MissingOpenPgpKeyPairException, SmackException.NotConnectedException, InterruptedException,
            SmackOpenPgpException, IOException {
        MultiMap<BareJid, OpenPgpV4Fingerprint> fingerprints = oursAndRecipientFingerprints();

        SigncryptElement preparedPayload = new SigncryptElement(
                Collections.<Jid>singleton(chat.getXmppAddressOfChatPartner()),
                payload);

        OpenPgpElement encryptedPayload;
        byte[] encryptedMessage;

        // Encrypt the payload
        try {
            encryptedMessage = cryptoProvider.signAndEncrypt(
                    preparedPayload,
                    singingKey,
                    fingerprints);
        } catch (MissingOpenPgpPublicKeyException e) {
            throw new AssertionError("Missing OpenPGP public key, even though this should not happen here.", e);
        }

        encryptedPayload = new OpenPgpElement(Base64.encodeToString(encryptedMessage));

        // Add encrypted payload to message
        message.addExtension(encryptedPayload);

        // Add additional information to the message
        message.addExtension(new ExplicitMessageEncryptionElement(
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        StoreHint.set(message);
        message.setBody("This message is encrypted using XEP-0374: OpenPGP for XMPP: Instant Messaging.");

        chat.send(message);
    }

    public void send(Message message, CharSequence body)
            throws MissingOpenPgpKeyPairException, SmackException.NotConnectedException, InterruptedException,
            SmackOpenPgpException, IOException {
        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body(null, body.toString()));
        send(message, payload);
    }

    public OpenPgpContentElement receive(OpenPgpElement element)
            throws XmlPullParserException, MissingOpenPgpKeyPairException, SmackOpenPgpException, IOException {
        byte[] decoded = Base64.decode(element.getEncryptedBase64MessageContent());

        DecryptedBytesAndMetadata decryptedBytes = cryptoProvider.decrypt(decoded, getJidOfChatPartner(), null);

        OpenPgpMessage openPgpMessage = new OpenPgpMessage(decryptedBytes.getBytes(),
                new OpenPgpMessage.Metadata(decryptedBytes.getDecryptionKey(),
                        decryptedBytes.getVerifiedSignatures()));

        return openPgpMessage.getOpenPgpContentElement();
    }

    private MultiMap<BareJid, OpenPgpV4Fingerprint> oursAndRecipientFingerprints() {
        MultiMap<BareJid, OpenPgpV4Fingerprint> fingerprints = new MultiMap<>();
        for (OpenPgpV4Fingerprint f : contactsFingerprints.getActiveKeys()) {
            fingerprints.put(contactsFingerprints.getJid(), f);
        }

        if (!contactsFingerprints.getJid().equals(ourFingerprints.getJid())) {
            for (OpenPgpV4Fingerprint f : ourFingerprints.getActiveKeys()) {
                fingerprints.put(ourFingerprints.getJid(), f);
            }
        }

        return fingerprints;
    }
}
