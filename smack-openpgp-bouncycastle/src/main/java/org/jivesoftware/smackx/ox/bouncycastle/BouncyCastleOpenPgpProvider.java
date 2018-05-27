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
package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.Util;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PublicKeySize;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.jxmpp.jid.BareJid;

public class BouncyCastleOpenPgpProvider implements OpenPgpProvider {

    private static final Logger LOGGER = Logger.getLogger(BouncyCastleOpenPgpProvider.class.getName());

    private final BareJid ourJid;
    private OpenPgpV4Fingerprint primaryKeyPairFingerprint;
    private InMemoryKeyring keyring;

    private final Map<BareJid, Set<OpenPgpV4Fingerprint>> contactsFingerprints = new HashMap<>();

    public BouncyCastleOpenPgpProvider(BareJid ourJid) {
        this.ourJid = ourJid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PubkeyElement createPubkeyElement(OpenPgpV4Fingerprint keyFingerprint)
            throws MissingOpenPgpPublicKeyException, CorruptedOpenPgpKeyException {
        // TODO: throw missing key exception
        try {
            PGPPublicKey pubKey = keyring.getPublicKeyRings().getPublicKey(Util.keyIdFromFingerprint(keyFingerprint));
            PubkeyElement.PubkeyDataElement dataElement = new PubkeyElement.PubkeyDataElement(
                    Base64.encode(pubKey.getEncoded()));
            return new PubkeyElement(dataElement, new Date());
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password) throws MissingOpenPgpKeyPairException, CorruptedOpenPgpKeyException {
        /*
        try {
            // Our unencrypted secret key
            PGPSecretKey secretKey;
            try {
                secretKey = ourKeys.getSecretKeyRings().getSecretKey(ourKeyId);
            } catch (NullPointerException e) {
                throw new MissingOpenPgpKeyPairException(ourJid);
            }

            PGPDigestCalculator calculator = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build()
                    .get(HashAlgorithmTags.SHA1);

            PBESecretKeyEncryptor encryptor = new JcePBESecretKeyEncryptorBuilder(
                    PGPSymmetricEncryptionAlgorithms.AES_256.getAlgorithmId())
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(password.toCharArray());

            PGPSecretKey encrypted = new PGPSecretKey(
                    secretKey.extractPrivateKey(null),
                    secretKey.getPublicKey(),
                    calculator,
                    true,
                    encryptor);

            byte[] base64 = Base64.encode(encrypted.getEncoded());

            return new SecretkeyElement(base64);

        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
        */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element) throws CorruptedOpenPgpKeyException {
        /*
        byte[] decoded = Base64.decode(element.getDataElement().getB64Data());

        try {
            InMemoryKeyring contactsKeyring = theirKeys.get(owner);
            if (contactsKeyring == null) {
                contactsKeyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
                theirKeys.put(owner, contactsKeyring);
            }

            contactsKeyring.addPublicKey(decoded);
        } catch (IOException | PGPException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner) {
        /*
        InMemoryKeyring contactsKeys = theirKeys.get(owner);
        for (OpenPgpV4Fingerprint fingerprint : listElement.getMetadata().keySet()) {
            byte[] asBytes = fingerprint.toString().getBytes(Charset.forName("UTF-8"));
            try {
                if (contactsKeys.getPublicKeyRings().getPublicKey(asBytes) == null) {
                    try {
                        PubkeyElement pubkey = PubSubDelegate.fetchPubkey(connection, owner, fingerprint);
                        storePublicKey(pubkey, owner);
                    } catch (PubSubException.NotAPubSubNodeException | PubSubException.NotALeafNodeException |
                            XMPPException.XMPPErrorException e) {
                        LOGGER.log(Level.WARNING, "Could not fetch public key " + fingerprint + " of " + owner + ".", e);
                    } catch (CorruptedOpenPgpKeyException e) {
                        LOGGER.log(Level.WARNING, "Key " + fingerprint + " of " + owner + " is corrupted and cannot be imported.", e);
                    }
                }
            } catch (PGPException | IOException e) {
                throw new CorruptedOpenPgpKeyException(e);
            }
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws CorruptedOpenPgpKeyException {
        /*
        byte[] encoded = Base64.decode(secretkeyElement.getB64Data());

        try {
            PGPDigestCalculatorProvider calculatorProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build();

            InMemoryKeyring keyring = KeyringConfigs.forGpgExportedKeys(
                    KeyringConfigCallbacks.withPassword(password));
            keyring.addSecretKey(encoded);
            for (PGPSecretKeyRing r : keyring.getSecretKeyRings()) {
                PGPSecretKey s = r.getSecretKey();
                PGPPrivateKey privateKey = s.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder(calculatorProvider).build(password.toCharArray()));
                PGPPublicKey publicKey = s.getPublicKey();
                PGPSecretKey secretKey = new PGPSecretKey(
                        privateKey,
                        publicKey,
                        calculatorProvider.get(PGPHashAlgorithms.SHA1.getAlgorithmId()),
                        true,
                        null);

                InMemoryKeyring newKeyring = KeyringConfigs.forGpgExportedKeys(
                        KeyringConfigCallbacks.withUnprotectedKeys());

                newKeyring.addPublicKey(secretKey.getPublicKey().getEncoded());
                newKeyring.addSecretKey(secretKey.getEncoded());

                ourKeys = newKeyring;
                ourKeyId = secretKey.getKeyID();

                InMemoryKeyring theirKeyRing = KeyringConfigs.forGpgExportedKeys(
                        KeyringConfigCallbacks.withUnprotectedKeys());
                theirKeyRing.addPublicKey(secretKey.getPublicKey().getEncoded());

                theirKeys.put(ourJid, theirKeyRing);
            }
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenPgpV4Fingerprint primaryOpenPgpKeyPairFingerprint() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeyPairFingerprints() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<OpenPgpV4Fingerprint> announcedOpenPgpKeyFingerprints(BareJid contact) {
        return null;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeysFingerprints(BareJid contact) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenPgpElement signAndEncrypt(SigncryptElement element,
                                         OpenPgpV4Fingerprint signingKey,
                                         Set<OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {
        /*
        if (encryptionKeys.isEmpty()) {
            throw new IllegalArgumentException("Set of recipients must not be empty");
        }

        InMemoryKeyring encryptionConfig = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());

        // Add all recipients public keys to encryption config
        for (BareJid recipient : recipients) {
            KeyringConfig c = theirKeys.get(recipient);
            for (PGPPublicKeyRing p : c.getPublicKeyRings()) {
                encryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
            }
        }

        // Add our public and secret keys to encryption config
        for (PGPPublicKeyRing p : ourKeys.getPublicKeyRings()) {
            encryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
        }
        for (PGPSecretKeyRing s : ourKeys.getSecretKeyRings()) {
            encryptionConfig.addSecretKey(s.getSecretKey().getEncoded());
        }

        String[] recipientUIDs = new String[recipients.size() + 1];
        int pos = 0;
        for (BareJid b : recipients) {
            recipientUIDs[pos++] = "xmpp:" + b.toString();
        }
        recipientUIDs[pos] = "xmpp:" + ourJid.toString();

        InputStream inputStream = element.toInputStream();
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();

        OutputStream encryptor = BouncyGPG.encryptToStream()
                .withConfig(encryptionConfig)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .withOxAlgorithms()
                .toRecipients(recipientUIDs)
                .andSignWith(ourKeyId)
                .binaryOutput()
                .andWriteTo(encryptedOut);

        Streams.pipeAll(inputStream, encryptor);
        encryptor.close();

        String base64 = Base64.encodeToString(encryptedOut.toByteArray());

        return new OpenPgpElement(base64);
        */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> possibleSigningKeys)
            throws MissingOpenPgpPublicKeyException, MissingOpenPgpKeyPairException {
        /*
        InMemoryKeyring decryptionConfig = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());

        // Add our secret keys to decryption config
        for (PGPSecretKeyRing s : ourKeys.getSecretKeyRings()) {
            decryptionConfig.addSecretKey(s.getSecretKey().getEncoded());
        }

        // Add their public keys to decryption config
        for (PGPPublicKeyRing p : theirKeys.get(sender).getPublicKeyRings()) {
            decryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
        }

        byte[] b64decoded = Base64.decode(element.getEncryptedBase64MessageContent());

        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(b64decoded);

        InputStream decrypted = BouncyGPG.decryptAndVerifyStream()
                .withConfig(decryptionConfig)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .andValidateSomeoneSigned()
                .fromEncryptedInputStream(encryptedIn);

        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();

        Streams.pipeAll(decrypted, decryptedOut);

        return new OpenPgpMessage(OpenPgpMessage.State.signcrypt, new String(decryptedOut.toByteArray(), Charset.forName("UTF-8")));
        */
        return null;
    }

    @Override
    public OpenPgpElement sign(SignElement element, OpenPgpV4Fingerprint singingKeyFingerprint)
            throws MissingOpenPgpKeyPairException {
        return null;
    }

    @Override
    public OpenPgpMessage verify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> singingKeyFingerprints)
            throws MissingOpenPgpPublicKeyException {
        return null;
    }

    @Override
    public OpenPgpElement encrypt(CryptElement element, Set<OpenPgpV4Fingerprint> encryptionKeyFingerprints)
            throws MissingOpenPgpPublicKeyException {
        return null;
    }

    @Override
    public OpenPgpMessage decrypt(OpenPgpElement element) throws MissingOpenPgpKeyPairException {
        return null;
    }

    public static OpenPgpV4Fingerprint getFingerprint(PGPPublicKey publicKey) {
        byte[] hex = Hex.encode(publicKey.getFingerprint());
        return new OpenPgpV4Fingerprint(hex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {
        /*
        try {
            PGPSecretKeyRing ourKey = generateKey(ourJid).generateSecretKeyRing();
            primaryKeyPairFingerprint = getFingerprint(ourKey.getPublicKey());
            ourKeys = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
            ourKeys.addSecretKey(ourKey.getSecretKey().getEncoded());
            ourKeys.addPublicKey(ourKey.getPublicKey().getEncoded());
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
        */
        return null;
    }

    public static PGPKeyRingGenerator generateKey(BareJid owner)
            throws NoSuchAlgorithmException, PGPException, NoSuchProviderException {
        PGPKeyRingGenerator generator = BouncyGPG.createKeyPair()
                .withRSAKeys()
                .ofSize(PublicKeySize.RSA._2048)
                .forIdentity("xmpp:" + owner.toString())
                .withoutPassphrase()
                .build();
        return generator;
    }

}
