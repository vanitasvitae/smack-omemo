/**
 *
 * Copyright 2017 Florian Schmaus.
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

import java.util.Set;

import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;

import org.jxmpp.jid.BareJid;

public interface OpenPgpProvider {

    /**
     * Sign and encrypt a {@link SigncryptElement} element for usage within the context of instant messaging.
     * The resulting {@link OpenPgpElement} contains a Base64 encoded, unarmored OpenPGP message,
     * which can be decrypted by each recipient, as well as by ourselves.
     * The message contains a signature made by our key.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#signcrypt">XEP-0373 §3</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link SigncryptElement} which contains the content of the message as plaintext.
     * @param recipients {@link Set} of {@link BareJid} of recipients.
     * @return encrypted {@link OpenPgpElement} which contains the encrypted, encoded message.
     * @throws Exception
     */
    OpenPgpElement signAndEncrypt(SigncryptElement element, Set<BareJid> recipients) throws Exception;

    /**
     * Decrypt an incoming {@link OpenPgpElement} which must contain a {@link SigncryptElement} and verify
     * the signature made by the sender in the context of instant messaging.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link OpenPgpElement} which contains an encrypted and signed {@link SigncryptElement}.
     * @param sender {@link BareJid} of the user which sent the message. This is also the user who signed the message.
     * @return decrypted {@link OpenPgpMessage} which contains the decrypted {@link SigncryptElement}.
     * @throws Exception
     */
    OpenPgpMessage decryptAndVerify(OpenPgpElement element, BareJid sender) throws Exception;

    /**
     * Sign a {@link SignElement} and pack it inside a {@link OpenPgpElement}.
     * The resulting {@link OpenPgpElement} contains the {@link SignElement} signed and base64 encoded.
     *
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link SignElement} which will be signed.
     * @return {@link OpenPgpElement} which contains the signed, Base64 encoded {@link SignElement}.
     * @throws Exception
     */
    OpenPgpElement sign(SignElement element) throws Exception;

    /**
     * Verify the signature on an incoming {@link OpenPgpElement} which must contain a {@link SignElement}.
     *
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element incoming {@link OpenPgpElement} which must contain a signed {@link SignElement}.
     * @param sender {@link BareJid} of the sender which also signed the message.
     * @return {@link OpenPgpMessage} which contains the decoded {@link SignElement}.
     * @throws Exception
     */
    OpenPgpMessage verify(OpenPgpElement element, BareJid sender) throws Exception;

    /**
     * Encrypt a {@link CryptElement} and pack it inside a {@link OpenPgpElement}.
     * The resulting {@link OpenPgpElement} contains the encrypted and Base64 encoded {@link CryptElement}
     * which can be decrypted by all recipients, as well as by ourselves.
     *
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element plaintext {@link CryptElement} which will be encrypted.
     * @param recipients {@link Set} of {@link BareJid} of recipients, which will be able to decrypt the message.
     * @return {@link OpenPgpElement} which contains the encrypted, Base64 encoded {@link CryptElement}.
     * @throws Exception
     */
    OpenPgpElement encrypt(CryptElement element, Set<BareJid> recipients) throws Exception;

    /**
     * Decrypt an incoming {@link OpenPgpElement} which must contain a {@link CryptElement}.
     * The resulting {@link OpenPgpMessage} will contain the decrypted {@link CryptElement}.
     *
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link OpenPgpElement} which contains the encrypted {@link CryptElement}.
     * @return {@link OpenPgpMessage} which contains the decrypted {@link CryptElement}.
     * @throws Exception
     */
    OpenPgpMessage decrypt(OpenPgpElement element) throws Exception;

    /**
     * Create a {@link PubkeyElement} which contains our exported OpenPGP public key.
     * The element can for example be published.
     *
     * @return {@link PubkeyElement} containing our public key.
     * @throws CorruptedOpenPgpKeyException if our public key can for some reason not be serialized.
     */
    PubkeyElement createPubkeyElement() throws CorruptedOpenPgpKeyException;

    /**
     * Process an incoming {@link PubkeyElement} of a contact or ourselves.
     * That typically includes importing/updating the key.
     *
     * @param element {@link PubkeyElement} which presumably contains the public key of the {@code owner}.
     * @param owner owner of the OpenPGP public key contained in the {@link PubkeyElement}.
     * @throws CorruptedOpenPgpKeyException if the key found in the {@link PubkeyElement}
     * can not be deserialized or imported.
     */
    void processPubkeyElement(PubkeyElement element, BareJid owner) throws CorruptedOpenPgpKeyException;

    /**
     * Process an incoming update to the OpenPGP metadata node.
     * That typically includes fetching announced keys of which we don't have a local copy yet,
     * as well as marking keys which are missing from the list as inactive.
     *
     * @param listElement {@link PublicKeysListElement} which contains a list of the keys of {@code owner}.
     * @param owner {@link BareJid} of the owner of the announced public keys.
     * @throws Exception
     */
    void processPublicKeysListElement(PublicKeysListElement listElement, BareJid owner) throws Exception;

    /**
     * Return the OpenPGP v4-fingerprint of our key in hexadecimal upper case.
     *
     * @return fingerprint
     * @throws CorruptedOpenPgpKeyException if for some reason the fingerprint cannot be derived from the key pair.
     */
    String getFingerprint() throws CorruptedOpenPgpKeyException;

    SecretkeyElement createSecretkeyElement(String password) throws CorruptedOpenPgpKeyException;

    void restoreSecretKeyElement(SecretkeyElement secretkeyElement, String password) throws CorruptedOpenPgpKeyException;
}
