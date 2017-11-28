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
package org.jivesoftware.smackx.omemo;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.MultipleCryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UntrustedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

public abstract class OmemoSessionManager<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    private static final Logger LOGGER = Logger.getLogger(OmemoSessionManager.class.getName());

    protected final OmemoManager.KnownBareJidGuard managerGuard;
    protected final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store;

    public OmemoSessionManager(OmemoManager.KnownBareJidGuard managerGuard,
                               OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store) {
        this.managerGuard = managerGuard;
        this.store = store;
    }

    public abstract byte[] doubleRatchetDecrypt(OmemoDevice sender, byte[] encryptedKey)
            throws CorruptedOmemoKeyException, NoRawSessionException, CryptoFailedException,
            UntrustedOmemoIdentityException;

    public abstract CiphertextTuple doubleRatchetEncrypt(OmemoDevice recipient, byte[] messageKey);

    /**
     * Try to decrypt the transported message key using the double ratchet session.
     *
     * @param element omemoElement
     * @return tuple of cipher generated from the unpacked message key and the authtag
     * @throws CryptoFailedException if decryption using the double ratchet fails
     * @throws NoRawSessionException if we have no session, but the element was NOT a PreKeyMessage
     */
    public CipherAndAuthTag retrieveMessageKeyAndAuthTag(OmemoDevice sender, OmemoElement element) throws CryptoFailedException,
            NoRawSessionException {
        int keyId = managerGuard.get().getDeviceId();
        byte[] unpackedKey = null;
        List<CryptoFailedException> decryptExceptions = new ArrayList<>();
        List<OmemoElement.OmemoHeader.Key> keys = element.getHeader().getKeys();
        // Find key with our ID.
        for (OmemoElement.OmemoHeader.Key k : keys) {
            if (k.getId() == keyId) {
                try {
                    unpackedKey = doubleRatchetDecrypt(sender, k.getData());
                    break;
                } catch (CryptoFailedException e) {
                    // There might be multiple keys with our id, but we can only decrypt one.
                    // So we can't throw the exception, when decrypting the first duplicate which is not for us.
                    decryptExceptions.add(e);
                } catch (CorruptedOmemoKeyException e) {
                    decryptExceptions.add(new CryptoFailedException(e));
                } catch (UntrustedOmemoIdentityException e) {
                    LOGGER.log(Level.WARNING, "Received message from " + sender + " contained unknown identityKey. Ignore message.", e);
                }
            }
        }

        if (unpackedKey == null) {
            if (!decryptExceptions.isEmpty()) {
                throw MultipleCryptoFailedException.from(decryptExceptions);
            }

            throw new CryptoFailedException("Transported key could not be decrypted, since no suitable message key " +
                    "was provided. Provides keys: " + keys);
        }

        // Split in AES auth-tag and key
        byte[] messageKey = new byte[16];
        byte[] authTag = null;

        if (unpackedKey.length == 32) {
            authTag = new byte[16];
            // copy key part into messageKey
            System.arraycopy(unpackedKey, 0, messageKey, 0, 16);
            // copy tag part into authTag
            System.arraycopy(unpackedKey, 16, authTag, 0,16);
        } else if (element.isKeyTransportElement() && unpackedKey.length == 16) {
            messageKey = unpackedKey;
        } else {
            throw new CryptoFailedException("MessageKey has wrong length: "
                    + unpackedKey.length + ". Probably legacy auth tag format.");
        }

        return new CipherAndAuthTag(messageKey, element.getHeader().getIv(), authTag);
    }

    /**
     * Use the symmetric key in cipherAndAuthTag to decrypt the payload of the omemoMessage.
     * The decrypted payload will be the body of the returned Message.
     *
     * @param element omemoElement containing a payload.
     * @param cipherAndAuthTag cipher and authentication tag.
     * @return Message containing the decrypted payload in its body.
     * @throws CryptoFailedException
     */
    public static Message decryptMessageElement(OmemoElement element, CipherAndAuthTag cipherAndAuthTag) throws CryptoFailedException {
        if (!element.isMessageElement()) {
            throw new IllegalArgumentException("decryptMessageElement cannot decrypt OmemoElement which is no MessageElement!");
        }

        if (cipherAndAuthTag.getAuthTag() == null || cipherAndAuthTag.getAuthTag().length != 16) {
            throw new CryptoFailedException("AuthenticationTag is null or has wrong length: "
                    + (cipherAndAuthTag.getAuthTag() == null ? "null" : cipherAndAuthTag.getAuthTag().length));
        }
        byte[] encryptedBody = new byte[element.getPayload().length + 16];
        byte[] payload = element.getPayload();
        System.arraycopy(payload, 0, encryptedBody, 0, payload.length);
        System.arraycopy(cipherAndAuthTag.getAuthTag(), 0, encryptedBody, payload.length, 16);

        try {
            String plaintext = new String(cipherAndAuthTag.getCipher().doFinal(encryptedBody), StringUtils.UTF8);
            Message decrypted = new Message();
            decrypted.setBody(plaintext);
            return decrypted;

        } catch (UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoFailedException("decryptMessageElement could not decipher message body: "
                    + e.getMessage());
        }
    }
}
