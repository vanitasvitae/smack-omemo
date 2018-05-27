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

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Set;

import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import org.jxmpp.jid.BareJid;

public interface OpenPgpProvider extends OpenPgpStore {

    /**
     * Sign and encrypt a {@link SigncryptElement} element for usage within the context of instant messaging.
     * The resulting {@link OpenPgpElement} contains a Base64 encoded, unarmored OpenPGP message,
     * which can be decrypted by each recipient, as well as by ourselves.
     * The message contains a signature made by our key.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#signcrypt">XEP-0373 §3</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link SigncryptElement} which contains the content of the message as plaintext.
     * @param signingKey {@link OpenPgpV4Fingerprint} of the signing key.
     * @param encryptionKeys {@link Set} containing all {@link OpenPgpV4Fingerprint}s of keys which will
     *                                  be able to decrypt the message.
     * @return encrypted {@link OpenPgpElement} which contains the encrypted, encoded message.
     * @throws MissingOpenPgpKeyPairException if the OpenPGP key pair with the given {@link OpenPgpV4Fingerprint}
     *                                        is not available.
     * @throws MissingOpenPgpKeyPairException if any of the OpenPGP public keys whose {@link OpenPgpV4Fingerprint}
     *                                        is listed in {@code encryptionKeys} is not available.
     */
    OpenPgpElement signAndEncrypt(SigncryptElement element,
                                  OpenPgpV4Fingerprint signingKey,
                                  Set<OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException;

    /**
     * Decrypt an incoming {@link OpenPgpElement} which must contain a {@link SigncryptElement} and verify
     * the signature made by the sender in the context of instant messaging.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link OpenPgpElement} which contains an encrypted and signed {@link SigncryptElement}.
     * @param sendersKeys {@link Set} of the senders {@link OpenPgpV4Fingerprint}s.
     *                               It is required, that one of those keys was used for signing the message.
     * @return decrypted {@link OpenPgpMessage} which contains the decrypted {@link SigncryptElement}.
     * @throws MissingOpenPgpKeyPairException if we have no OpenPGP key pair to decrypt the message.
     * @throws MissingOpenPgpPublicKeyException if we do not have the public OpenPGP key of the sender to
     *                                          verify the signature on the message.
     */
    OpenPgpMessage decryptAndVerify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> sendersKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException;

    /**
     * Sign a {@link SignElement} and pack it inside a {@link OpenPgpElement}.
     * The resulting {@link OpenPgpElement} contains the {@link SignElement} signed and base64 encoded.
     * <br>
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link SignElement} which will be signed.
     * @param singingKeyFingerprint {@link OpenPgpV4Fingerprint} of the key that is used for signing.
     * @return {@link OpenPgpElement} which contains the signed, Base64 encoded {@link SignElement}.
     * @throws MissingOpenPgpKeyPairException if we don't have the key pair for the
     *                                        {@link OpenPgpV4Fingerprint} available.
     */
    OpenPgpElement sign(SignElement element, OpenPgpV4Fingerprint singingKeyFingerprint)
            throws MissingOpenPgpKeyPairException;

    /**
     * Verify the signature on an incoming {@link OpenPgpElement} which must contain a {@link SignElement}.
     * <br>
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element incoming {@link OpenPgpElement} which must contain a signed {@link SignElement}.
     * @param singingKeyFingerprints {@link Set} of the senders key {@link OpenPgpV4Fingerprint}s.
     *                                          It is required that one of those keys was used to sign
     *                                          the message.
     * @return {@link OpenPgpMessage} which contains the decoded {@link SignElement}.
     * @throws MissingOpenPgpPublicKeyException if we don't have the signers public key which signed
     *                                          the message available.
     */
    OpenPgpMessage verify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> singingKeyFingerprints)
            throws MissingOpenPgpPublicKeyException;

    /**
     * Encrypt a {@link CryptElement} and pack it inside a {@link OpenPgpElement}.
     * The resulting {@link OpenPgpElement} contains the encrypted and Base64 encoded {@link CryptElement}
     * which can be decrypted by all recipients, as well as by ourselves.
     * <br>
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element plaintext {@link CryptElement} which will be encrypted.
     * @param encryptionKeyFingerprints {@link Set} of {@link OpenPgpV4Fingerprint}s of the keys which
     *                                  are used for encryption.
     * @return {@link OpenPgpElement} which contains the encrypted, Base64 encoded {@link CryptElement}.
     * @throws MissingOpenPgpPublicKeyException if any of the OpenPGP public keys whose
     *                                          {@link OpenPgpV4Fingerprint} is listed in {@code encryptionKeys}
     *                                          is not available.
     */
    OpenPgpElement encrypt(CryptElement element, Set<OpenPgpV4Fingerprint> encryptionKeyFingerprints)
            throws MissingOpenPgpPublicKeyException;

    /**
     * Decrypt an incoming {@link OpenPgpElement} which must contain a {@link CryptElement}.
     * The resulting {@link OpenPgpMessage} will contain the decrypted {@link CryptElement}.
     * <br>
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     * @param element {@link OpenPgpElement} which contains the encrypted {@link CryptElement}.
     * @return {@link OpenPgpMessage} which contains the decrypted {@link CryptElement}.
     * @throws MissingOpenPgpKeyPairException if we don't have an OpenPGP key pair available that to decrypt
     *                                        the message.
     */
    OpenPgpMessage decrypt(OpenPgpElement element)
            throws MissingOpenPgpKeyPairException;

    /**
     * Create a fresh OpenPGP key pair with the {@link BareJid} of the user prefixed by "xmpp:" as user-id
     * (example: {@code "xmpp:juliet@capulet.lit"}).
     * Store the key pair in persistent storage and return the public keys {@link OpenPgpV4Fingerprint}.
     *
     * @throws NoSuchAlgorithmException if a Hash algorithm is not available
     * @throws NoSuchProviderException id no suitable cryptographic provider (for example BouncyCastleProvider)
     *                                 is registered.
     */
    OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException;
}
