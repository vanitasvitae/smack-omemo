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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.callback.SmackMissingOpenPgpPublicKeyCallback;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.DecryptedBytesAndMetadata;
import org.jivesoftware.smackx.ox.util.KeyBytesAndFingerprint;

import org.jxmpp.jid.BareJid;

public interface OpenPgpProvider {

    /**
     * Sign and encrypt a {@link SigncryptElement} element for usage within the context of instant messaging.
     * The resulting byte array can be decrypted by each recipient, as well as all devices of the user.
     * The message contains a signature made by our key.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#signcrypt">XEP-0373 §3</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     *
     * @param element {@link SigncryptElement} which contains the content of the message as plaintext.
     * @param signingKey {@link OpenPgpV4Fingerprint} of the signing key.
     * @param encryptionKeys {@link MultiMap} containing all {@link OpenPgpV4Fingerprint}s of recipients which will
     *                                  be able to decrypt the message.
     * @return encrypted and signed data which contains the encrypted, encoded message.
     *
     * @throws MissingOpenPgpKeyPairException if the OpenPGP key pair with the given {@link OpenPgpV4Fingerprint}
     *                                        is not available.
     * @throws MissingOpenPgpKeyPairException if any of the OpenPGP public keys whose {@link OpenPgpV4Fingerprint}
     *                                        is listed in {@code encryptionKeys} is not available.
     */
    byte[] signAndEncrypt(SigncryptElement element,
                          OpenPgpV4Fingerprint signingKey,
                          MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException, SmackOpenPgpException, IOException;

    /**
     * Sign a {@link SignElement} with the users signing key.
     * The resulting byte array contains the signed byte representation of the {@link SignElement}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     *
     * @param element {@link SignElement} which will be signed.
     * @param singingKeyFingerprint {@link OpenPgpV4Fingerprint} of the key that is used for signing.
     * @return byte array which contains the signed {@link SignElement}.
     *
     * @throws MissingOpenPgpKeyPairException if we don't have the key pair for the
     *                                        {@link OpenPgpV4Fingerprint} available.
     */
    byte[] sign(SignElement element, OpenPgpV4Fingerprint singingKeyFingerprint)
            throws MissingOpenPgpKeyPairException, IOException, SmackOpenPgpException;

    /**
     * Encrypt a {@link CryptElement} for all keys which fingerprints are contained in
     * {@code encryptionKeyFingerprints}.
     * The resulting byte array contains the encrypted {@link CryptElement}
     * which can be decrypted by all recipients, as well as by ourselves.
     * <br>
     * Note: DO NOT use this method in the context of instant messaging, as XEP-0374 forbids that.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html#openpgp-secured-im">XEP-0374 §2.1</a>
     *
     * @param element plaintext {@link CryptElement} which will be encrypted.
     * @param encryptionKeyFingerprints {@link MultiMap} of recipients and {@link OpenPgpV4Fingerprint}s of the
     *                                  keys which are used for encryption.
     * @return byte array which contains the encrypted {@link CryptElement}.
     * @throws MissingOpenPgpPublicKeyException if any of the OpenPGP public keys whose
     *                                          {@link OpenPgpV4Fingerprint} is listed in {@code encryptionKeys}
     *                                          is not available.
     */
    byte[] encrypt(CryptElement element, MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeyFingerprints)
            throws MissingOpenPgpPublicKeyException, IOException, SmackOpenPgpException;

    /**
     * Process an incoming {@link OpenPgpElement}.
     * If its content is encrypted ({@link CryptElement} or {@link SigncryptElement}), the content will be decrypted.
     * If its content is signed ({@link SignElement} or {@link SigncryptElement}), signatures are verified using
     * the announced public keys of the sender.
     * The resulting byte array will contain the decrypted {@link OpenPgpContentElement}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#exchange">XEP-0373 §3.1</a>
     *
     * @param bytes byte array which contains the encrypted {@link OpenPgpContentElement}.
     * @return byte array which contains the decrypted {@link OpenPgpContentElement}, as well as metadata.
     *
     * @throws MissingOpenPgpKeyPairException if we don't have an OpenPGP key pair available that to decrypt
     *                                        the message.
     */
    DecryptedBytesAndMetadata decrypt(byte[] bytes, BareJid sender, SmackMissingOpenPgpPublicKeyCallback missingPublicKeyCallback)
            throws MissingOpenPgpKeyPairException, SmackOpenPgpException;

    byte[] symmetricallyEncryptWithPassword(byte[] bytes, String password) throws SmackOpenPgpException, IOException;

    /**
     * Decrypt a symmetrically encrypted array of data using the provided password.
     *
     * @param bytes symmetrically encrypted data
     * @param password password for decryption
     * @return decrypted data
     * @throws SmackOpenPgpException if the password is incorrect
     * @throws IOException io is dangerous
     */
    byte[] symmetricallyDecryptWithPassword(byte[] bytes, String password) throws SmackOpenPgpException, IOException;

    /**
     * Generate a fresh OpenPGP key pair.
     *
     * @param owner JID of the keys owner.
     * @return byte array representation + {@link OpenPgpV4Fingerprint} of the generated key pair.
     */
    KeyBytesAndFingerprint generateOpenPgpKeyPair(BareJid owner)
            throws SmackOpenPgpException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException;

    /**
     * Import a public key. The bytes are expected to be decoded from base64.
     * @param owner
     * @param bytes
     * @return
     * @throws MissingUserIdOnKeyException
     * @throws IOException
     * @throws SmackOpenPgpException
     */
    OpenPgpV4Fingerprint importPublicKey(BareJid owner, byte[] bytes)
            throws MissingUserIdOnKeyException, IOException, SmackOpenPgpException;

    OpenPgpV4Fingerprint importSecretKey(BareJid owner, byte[] bytes)
            throws MissingUserIdOnKeyException, SmackOpenPgpException, IOException;

    OpenPgpV4Fingerprint importSecretKey(byte[] bytes)
            throws MissingUserIdOnKeyException, SmackOpenPgpException, IOException;

    OpenPgpStore getStore();
}
