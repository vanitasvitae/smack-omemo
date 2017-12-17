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
package org.jivesoftware.smackx.omemo.util;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.CIPHERMODE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYLENGTH;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYTYPE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.PROVIDER;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoRatchet;
import org.jivesoftware.smackx.omemo.element.OmemoVAxolotlElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;


/**
 * Class used to build OMEMO messages.
 *
 * @param <T_IdKeyPair> IdentityKeyPair class
 * @param <T_IdKey>     IdentityKey class
 * @param <T_PreKey>    PreKey class
 * @param <T_SigPreKey> SignedPreKey class
 * @param <T_Sess>      Session class
 * @param <T_Addr>      Address class
 * @param <T_ECPub>     Elliptic Curve PublicKey class
 * @param <T_Bundle>    Bundle class
 * @param <T_Ciph>      Cipher class
 * @author Paul Schaub
 */
public class OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    private final OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet;
    private final OmemoManager.LoggedInOmemoManager managerGuard;

    private byte[] messageKey = generateKey();
    private byte[] initializationVector = generateIv();

    private byte[] ciphertextMessage;
    private final ArrayList<OmemoVAxolotlElement.OmemoHeader.Key> keys = new ArrayList<>();

    /**
     * Create a OmemoMessageBuilder.
     *
     * @param managerGuard      OmemoManager of our device.
     * @param ratchet           OmemoRatchet.
     * @param aesKey            AES key that will be transported to the recipient. This is used eg. to encrypt the body.
     * @param iv                IV
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    public OmemoMessageBuilder(OmemoManager.LoggedInOmemoManager managerGuard,
                               OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet,
                               byte[] aesKey, byte[] iv)
            throws NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException,
            UnsupportedEncodingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        this.managerGuard = managerGuard;
        this.ratchet = ratchet;
        this.messageKey = aesKey;
        this.initializationVector = iv;
    }

    /**
     * Create a new OmemoMessageBuilder with random IV and AES key.
     *
     * @param managerGuard      omemoManager of our device.
     * @param ratchet    omemoSessionManager.
     * @param message           Messages body.
     * @throws NoSuchPaddingException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws UnsupportedEncodingException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     */
    public OmemoMessageBuilder(OmemoManager.LoggedInOmemoManager managerGuard,
                               OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet,
                               String message)
            throws NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
            UnsupportedEncodingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        this.managerGuard = managerGuard;
        this.ratchet = ratchet;
        this.setMessage(message);
    }

    /**
     * Create an AES messageKey and use it to encrypt the message.
     * Optionally append the Auth Tag of the encrypted message to the messageKey afterwards.
     *
     * @param message content of the message
     * @throws NoSuchPaddingException               When no Cipher could be instantiated.
     * @throws NoSuchAlgorithmException             when no Cipher could be instantiated.
     * @throws NoSuchProviderException              when BouncyCastle could not be found.
     * @throws InvalidAlgorithmParameterException   when the Cipher could not be initialized
     * @throws InvalidKeyException                  when the generated key is invalid
     * @throws UnsupportedEncodingException         when UTF8 is unavailable
     * @throws BadPaddingException                  when cipher.doFinal gets wrong padding
     * @throws IllegalBlockSizeException            when cipher.doFinal gets wrong Block size.
     */
    public void setMessage(String message) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        if (message == null) {
            return;
        }

        // Encrypt message body
        SecretKey secretKey = new SecretKeySpec(messageKey, KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);
        Cipher cipher = Cipher.getInstance(CIPHERMODE, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] body;
        byte[] ciphertext;

        body = (message.getBytes(StringUtils.UTF8));
        ciphertext = cipher.doFinal(body);

        byte[] clearKeyWithAuthTag = new byte[messageKey.length + 16];
        byte[] cipherTextWithoutAuthTag = new byte[ciphertext.length - 16];

        System.arraycopy(messageKey, 0, clearKeyWithAuthTag, 0, 16);
        System.arraycopy(ciphertext, 0, cipherTextWithoutAuthTag, 0, cipherTextWithoutAuthTag.length);
        System.arraycopy(ciphertext, ciphertext.length - 16, clearKeyWithAuthTag, 16, 16);

        ciphertextMessage = cipherTextWithoutAuthTag;
        messageKey = clearKeyWithAuthTag;
    }

    /**
     * Add a new recipient device to the message.
     *
     * @param device recipient device
     * @throws CryptoFailedException                when encrypting the messageKey fails
     * @throws UndecidedOmemoIdentityException
     * @throws CorruptedOmemoKeyException
     */
    public void addRecipient(OmemoDevice device)
            throws CryptoFailedException, UndecidedOmemoIdentityException, CorruptedOmemoKeyException,
            CannotEstablishOmemoSessionException {
        addRecipient(device, false);
    }

    /**
     * Add a new recipient device to the message.
     * @param contactsDevice recipient device
     * @param ignoreTrust ignore current trust state? Useful for keyTransportMessages that are sent to repair a session
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     * @throws CorruptedOmemoKeyException
     */
    public void addRecipient(OmemoDevice contactsDevice, boolean ignoreTrust) throws
            CryptoFailedException, UndecidedOmemoIdentityException, CorruptedOmemoKeyException,
            CannotEstablishOmemoSessionException {

        OmemoFingerprint fingerprint;
        try {
            fingerprint = managerGuard.get().getFingerprint(contactsDevice);
        } catch (SmackException.NotLoggedInException e) {
            throw new AssertionError("This should never happen.");
        }

        if (!ignoreTrust && !managerGuard.get().isDecidedOmemoIdentity(contactsDevice, fingerprint)) {
            // Warn user of undecided device
            throw new UndecidedOmemoIdentityException(contactsDevice);
        }

        if (ignoreTrust || managerGuard.get().isTrustedOmemoIdentity(contactsDevice, fingerprint)) {
            // Encrypt key and save to header
            CiphertextTuple encryptedKey = ratchet.doubleRatchetEncrypt(contactsDevice, messageKey);
            keys.add(new OmemoVAxolotlElement.OmemoHeader.Key(encryptedKey.getCiphertext(), contactsDevice.getDeviceId(), encryptedKey.isPreKeyMessage()));
        }
    }

    /**
     * Assemble an OmemoMessageElement from the current state of the builder.
     *
     * @return OmemoMessageElement
     */
    public OmemoVAxolotlElement finish() {
        OmemoVAxolotlElement.OmemoHeader header = new OmemoVAxolotlElement.OmemoHeader(
                managerGuard.get().getDeviceId(),
                keys,
                initializationVector
        );
        return new OmemoVAxolotlElement(header, ciphertextMessage);
    }

    /**
     * Generate a new AES key used to encrypt the message.
     *
     * @return new AES key
     * @throws NoSuchAlgorithmException
     */
    public static byte[] generateKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEYTYPE);
        generator.init(KEYLENGTH);
        return generator.generateKey().getEncoded();
    }

    /**
     * Generate a 16 byte initialization vector for AES encryption.
     *
     * @return iv
     */
    public static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }

    public byte[] getCiphertextMessage() {
        return ciphertextMessage;
    }

    public byte[] getMessageKey() {
        return messageKey;
    }
}
