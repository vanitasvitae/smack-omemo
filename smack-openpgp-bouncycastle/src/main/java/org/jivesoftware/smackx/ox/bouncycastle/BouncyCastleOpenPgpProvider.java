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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PGPHashAlgorithms;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PGPSymmetricEncryptionAlgorithms;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PublicKeySize;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.XmppKeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;

public class BouncyCastleOpenPgpProvider implements OpenPgpProvider {

    private final BareJid ourJid;
    private InMemoryKeyring ourKeys;
    private Long ourKeyId;
    private final Map<BareJid, InMemoryKeyring> theirKeys = new HashMap<>();

    public BouncyCastleOpenPgpProvider(BareJid ourJid) throws IOException, PGPException, NoSuchAlgorithmException {
        this.ourJid = ourJid;
    }

    @Override
    public PubkeyElement createPubkeyElement() throws CorruptedOpenPgpKeyException {
        try {
            PGPPublicKey pubKey = ourKeys.getPublicKeyRings().getPublicKey(ourKeyId);
            PubkeyElement.PubkeyDataElement dataElement = new PubkeyElement.PubkeyDataElement(
                    Base64.encode(pubKey.getEncoded()));
            return new PubkeyElement(dataElement, new Date());
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
    }

    @Override
    public SecretkeyElement createSecretkeyElement(String password) throws CorruptedOpenPgpKeyException {
        try {
            // Our unencrypted secret key
            PGPSecretKey secretKey = ourKeys.getSecretKeyRings().getSecretKey(ourKeyId);

            PGPDigestCalculator calculator = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyGPG.getProvider())
                    .build()
                    .get(HashAlgorithmTags.SHA1);

            PBESecretKeyEncryptor encryptor = new JcePBESecretKeyEncryptorBuilder(
                    PGPSymmetricEncryptionAlgorithms.AES_256.getAlgorithmId())
                    .setProvider(BouncyGPG.getProvider())
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
    }

    @Override
    public void processPubkeyElement(PubkeyElement element, BareJid owner) throws CorruptedOpenPgpKeyException {
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
    }

    @Override
    public void processPublicKeysListElement(PublicKeysListElement listElement, BareJid owner) throws Exception {

    }

    @Override
    public void restoreSecretKeyElement(SecretkeyElement secretkeyElement, String password)
            throws CorruptedOpenPgpKeyException {
        byte[] encoded = Base64.decode(secretkeyElement.getB64Data());

        try {
            PGPDigestCalculatorProvider calculatorProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyGPG.getProvider())
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
    }

    @Override
    public OpenPgpElement signAndEncrypt(SigncryptElement element, Set<BareJid> recipients)
            throws Exception {
        if (recipients.isEmpty()) {
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
                .andSignWith("xmpp:" + ourJid.toString())
                .binaryOutput()
                .andWriteTo(encryptedOut);

        Streams.pipeAll(inputStream, encryptor);
        encryptor.close();

        String base64 = Base64.encodeToString(encryptedOut.toByteArray());

        return new OpenPgpElement(base64);
    }

    @Override
    public OpenPgpElement sign(SignElement element) throws Exception {

        throw new SmackException.FeatureNotSupportedException("Feature not implemented for now.");
        /*
        InMemoryKeyring signingConfig = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());

        // Add our secret keys to signing config
        for (PGPSecretKeyRing s : ourKeys.getSecretKeyRings()) {
            signingConfig.addSecretKey(s.getSecretKey().getEncoded());
        }

        InputStream inputStream = element.toInputStream();
        // TODO: Implement

        return null;
        */
    }

    @Override
    public OpenPgpMessage verify(OpenPgpElement element, BareJid sender) throws Exception {
        // TODO: Implement
        throw new SmackException.FeatureNotSupportedException("Feature not implemented for now.");
    }

    @Override
    public OpenPgpMessage decrypt(OpenPgpElement element) throws Exception {
        throw new SmackException.FeatureNotSupportedException("Feature not implemented for now.");
        /*
        InMemoryKeyring decryptionConfig = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());

        // Add our secret keys to decryption config
        for (PGPSecretKeyRing s : ourKeys.getSecretKeyRings()) {
            decryptionConfig.addSecretKey(s.getSecretKey().getEncoded());
        }

        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(
                element.getEncryptedBase64MessageContent().getBytes(Charset.forName("UTF-8")));

        InputStream decrypted = BouncyGPG.decryptAndVerifyStream()
                .withConfig(decryptionConfig)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .andIgnoreSignatures()
                .fromEncryptedInputStream(encryptedIn);

        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();

        Streams.pipeAll(decrypted, decryptedOut);

        return new OpenPgpMessage(OpenPgpMessage.State.crypt, new String(decryptedOut.toByteArray(), Charset.forName("UTF-8")));
        */
    }

    @Override
    public OpenPgpElement encrypt(CryptElement element, Set<BareJid> recipients) throws Exception {
        throw new SmackException.FeatureNotSupportedException("Feature not implemented for now.");
        /*
        if (recipients.isEmpty()) {
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

        // Add our public keys to encryption config
        for (PGPPublicKeyRing p : ourKeys.getPublicKeyRings()) {
            encryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
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
                .andDoNotSign()
                .binaryOutput()
                .andWriteTo(encryptedOut);

        Streams.pipeAll(inputStream, encryptor);
        encryptor.close();

        String base64 = Base64.encodeToString(encryptedOut.toByteArray());

        return new OpenPgpElement(base64);
        */
    }

    @Override
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, BareJid sender) throws Exception {
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
    }

    @Override
    public String getFingerprint() throws CorruptedOpenPgpKeyException {
        try {
            return new String(Hex.encode(ourKeys.getKeyFingerPrintCalculator()
                    .calculateFingerprint(ourKeys.getPublicKeyRings().getPublicKey(ourKeyId)
                            .getPublicKeyPacket())), Charset.forName("UTF-8")).toUpperCase();
        } catch (IOException | PGPException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
    }

    @Override
    public void createAndUseKey() throws CorruptedOpenPgpKeyException, NoSuchAlgorithmException {
        try {
            PGPSecretKeyRing ourKey = generateKey(ourJid).generateSecretKeyRing();
            ourKeyId = ourKey.getPublicKey().getKeyID();
            ourKeys = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
            ourKeys.addSecretKey(ourKey.getSecretKey().getEncoded());
            ourKeys.addPublicKey(ourKey.getPublicKey().getEncoded());
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
    }

    public static PGPKeyRingGenerator generateKey(BareJid owner) throws NoSuchAlgorithmException, PGPException {
        PGPKeyRingGenerator generator = BouncyGPG.createKeyPair()
                .withRSAKeys()
                .ofSize(PublicKeySize.RSA._2048)
                .forIdentity("xmpp:" + owner.toString())
                .withoutPassphrase()
                .build();
        return generator;
    }

}
