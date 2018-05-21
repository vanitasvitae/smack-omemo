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

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PublicKeySize;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.XmppKeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;

public class BouncyCastleOpenPgpProvider implements OpenPgpProvider {

    private final BareJid ourJid;
    private final InMemoryKeyring ourKeys;
    private final Long ourKeyId;
    private final Map<BareJid, InMemoryKeyring> theirKeys = new HashMap<>();

    public BouncyCastleOpenPgpProvider(BareJid ourJid) throws IOException, PGPException, NoSuchAlgorithmException {
        this.ourJid = ourJid;
        PGPSecretKeyRing ourKey = generateKey(ourJid).generateSecretKeyRing();
        ourKeyId = ourKey.getPublicKey().getKeyID();
        ourKeys = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
        ourKeys.addSecretKey(ourKey.getSecretKey().getEncoded());
        ourKeys.addPublicKey(ourKey.getPublicKey().getEncoded());
    }

    @Override
    public PubkeyElement createPubkeyElement() throws IOException, PGPException {
        PGPPublicKey pubKey = ourKeys.getPublicKeyRings().getPublicKey(ourKeyId);
        PubkeyElement.PubkeyDataElement dataElement = new PubkeyElement.PubkeyDataElement(
                Base64.encode(pubKey.getEncoded()));
        return new PubkeyElement(dataElement, new Date());
    }

    @Override
    public void processPubkeyElement(PubkeyElement element, BareJid jid) throws IOException, PGPException {
        byte[] decoded = Base64.decode(element.getDataElement().getB64Data());

        InMemoryKeyring contactsKeyring = theirKeys.get(jid);
        if (contactsKeyring == null) {
            contactsKeyring = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
            theirKeys.put(jid, contactsKeyring);
        }

        contactsKeyring.addPublicKey(decoded);
    }

    @Override
    public void processPublicKeysListElement(PublicKeysListElement listElement, BareJid from) throws Exception {

    }

    @Override
    public OpenPgpElement signAndEncrypt(InputStream inputStream, Set<BareJid> recipients)
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

        // Add our keys to encryption config
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
    public OpenPgpElement sign(InputStream inputStream) throws Exception {
        return null;
    }

    @Override
    public OpenPgpElement encrypt(InputStream inputStream, Set<BareJid> recipients) throws Exception {
        return null;
    }

    @Override
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, BareJid sender) throws Exception {
        InMemoryKeyring decryptionConfig = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());

        // Add our keys to decryption config
        for (PGPPublicKeyRing p : ourKeys.getPublicKeyRings()) {
            decryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
        }
        for (PGPSecretKeyRing s : ourKeys.getSecretKeyRings()) {
            decryptionConfig.addSecretKey(s.getSecretKey().getEncoded());
        }

        // Add their keys to decryption config
        for (PGPPublicKeyRing p : theirKeys.get(sender).getPublicKeyRings()) {
            decryptionConfig.addPublicKey(p.getPublicKey().getEncoded());
        }

        ByteArrayInputStream encryptedIn = new ByteArrayInputStream(
                element.getEncryptedBase64MessageContent().getBytes(Charset.forName("UTF-8")));

        InputStream decrypted = BouncyGPG.decryptAndVerifyStream()
                .withConfig(decryptionConfig)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .andValidateSomeoneSigned()
                .fromEncryptedInputStream(encryptedIn);

        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();

        Streams.pipeAll(decrypted, decryptedOut);

        return new OpenPgpMessage(new String(decryptedOut.toByteArray(), Charset.forName("UTF-8")));
    }

    @Override
    public String getFingerprint() throws IOException, PGPException {
        return new String(Hex.encode(ourKeys.getKeyFingerPrintCalculator()
                .calculateFingerprint(ourKeys.getPublicKeyRings().getPublicKey(ourKeyId)
                        .getPublicKeyPacket())), Charset.forName("UTF-8")).toUpperCase();
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
