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
package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.Util;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PGPHashAlgorithms;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.algorithms.PGPSymmetricEncryptionAlgorithms;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallback;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
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
import org.jxmpp.jid.BareJid;

public class FileBasedBcOpenPgpStore implements BCOpenPgpStore {

    private static final Logger LOGGER = Logger.getLogger(FileBasedBcOpenPgpStore.class.getName());

    private final File basePath;
    private final BareJid user;
    private final InMemoryKeyring keyringConfig;
    private final KeyringConfigCallback configCallback;
    private OpenPgpV4Fingerprint primaryKeyFingerprint;

    public FileBasedBcOpenPgpStore(File basePath, BareJid user, KeyringConfigCallback passwordCallback)
            throws IOException, PGPException {
        this.basePath = basePath;
        this.user = user;

        File pub = publicKeyringPath();
        if (!pub.exists()) {
            pub.getParentFile().mkdirs();
            pub.createNewFile();
        }

        File sec = secretKeyringPath();
        if (!sec.exists()) {
            sec.createNewFile();
        }

        configCallback = passwordCallback;
        keyringConfig = KeyringConfigs.forGpgExportedKeys(configCallback);

        addPublicKeysFromFile(keyringConfig, pub, configCallback);
        PGPPublicKey lastAdded = addSecretKeysFromFile(keyringConfig, sec, configCallback);

        if (lastAdded != null) {
            primaryKeyFingerprint = BCOpenPgpProvider.getFingerprint(lastAdded);
        }
    }

    @Override
    public OpenPgpV4Fingerprint primaryOpenPgpKeyPairFingerprint() {
        return primaryKeyFingerprint;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeyPairFingerprints() {
        Set<OpenPgpV4Fingerprint> availableKeyPairs = new HashSet<>();
        try {
            for (PGPSecretKeyRing secRing : keyringConfig.getSecretKeyRings()) {
                for (PGPSecretKey secKey : secRing) {
                    availableKeyPairs.add(BCOpenPgpProvider.getFingerprint(secKey.getPublicKey()));
                }
            }
        } catch (IOException | PGPException e) {
            LOGGER.log(Level.SEVERE, "Error going through available key pair.", e);
        }
        return availableKeyPairs;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> announcedOpenPgpKeyFingerprints(BareJid contact) {
        Set<OpenPgpV4Fingerprint> announcedKeys = new HashSet<>();
        File listPath = contactsList(contact);
        if (listPath.exists() && listPath.isFile()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream(listPath), "UTF8"));

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    try {
                        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(line);
                        announcedKeys.add(fingerprint);
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.INFO, "Skip malformed fingerprint " + line + " of " + contact.toString());
                    }
                }
                reader.close();
            } catch (IOException e) {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        // Ignore
                    }
                }
            }
        }
        return announcedKeys;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpPublicKeysFingerprints(BareJid contact)
            throws SmackOpenPgpException {
        Set<OpenPgpV4Fingerprint> availableKeys = new HashSet<>();
        try {
            Iterator<PGPPublicKeyRing> ringIterator = keyringConfig.getPublicKeyRings().getKeyRings("xmpp:" + contact.toString());
            while (ringIterator.hasNext()) {
                PGPPublicKeyRing ring = ringIterator.next();
                Iterator<PGPPublicKey> keyIterator = ring.getPublicKeys();
                while (keyIterator.hasNext()) {
                    PGPPublicKey key = keyIterator.next();
                    if (key.isEncryptionKey()) {
                        availableKeys.add(BCOpenPgpProvider.getFingerprint(key));
                    }
                }
            }
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }
        return availableKeys;
    }

    @Override
    public void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner) {
        File listPath = contactsList(owner);
        try {
            if (!listPath.exists()) {
                listPath.getParentFile().mkdirs();
                listPath.createNewFile();
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(listPath), "UTF8"));

                    for (OpenPgpV4Fingerprint fingerprint : listElement.getMetadata().keySet()) {
                        writer.write(fingerprint.toString());
                        writer.newLine();
                    }

                    writer.close();

                } catch (IOException e) {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing list of announced keys for " + owner.toString(), e);
        }
    }

    @Override
    public PubkeyElement createPubkeyElement(OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException, SmackOpenPgpException {
        try {
            PGPPublicKey publicKey = keyringConfig.getPublicKeyRings().getPublicKey(Util.keyIdFromFingerprint(fingerprint));
            if (publicKey == null) {
                throw new MissingOpenPgpPublicKeyException(user, fingerprint);
            }
            byte[] base64 = Base64.encode(publicKey.getEncoded());
            return new PubkeyElement(new PubkeyElement.PubkeyDataElement(base64), new Date());
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    @Override
    public void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element)
            throws SmackOpenPgpException {
        byte[] base64decoded = Base64.decode(element.getDataElement().getB64Data());
        try {
            keyringConfig.addPublicKey(base64decoded);
            writePublicKeysToFile(keyringConfig, publicKeyringPath());
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Public Key with ID " + fingerprint.toString() + " of " +
                    owner + " is already in memory. Skip.");
        }
    }

    @Override
    public SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password)
            throws MissingOpenPgpKeyPairException, SmackOpenPgpException {

        PGPDigestCalculator calculator;
        try {
            calculator = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build()
                    .get(HashAlgorithmTags.SHA1);
        } catch (PGPException e) {
            throw new AssertionError(e);
        }

        PBESecretKeyEncryptor encryptor = new JcePBESecretKeyEncryptorBuilder(
                PGPSymmetricEncryptionAlgorithms.AES_256.getAlgorithmId())
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(password.toCharArray());

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            for (OpenPgpV4Fingerprint fingerprint : fingerprints) {
                // Our unencrypted secret key
                PGPSecretKey secretKey = keyringConfig.getSecretKeyRings()
                        .getSecretKey(Util.keyIdFromFingerprint(fingerprint));

                if (secretKey == null) {
                    // TODO: Close streams
                    throw new MissingOpenPgpKeyPairException(user);
                }

                PGPSecretKey encrypted = new PGPSecretKey(
                        secretKey.extractPrivateKey(null),
                        secretKey.getPublicKey(),
                        calculator,
                        true,
                        encryptor);

                buffer.write(encrypted.getEncoded());
            }

            return new SecretkeyElement(Base64.encode(buffer.toByteArray()));

        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    @Override
    public void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws SmackOpenPgpException, InvalidBackupCodeException {
        byte[] base64Decoded = Base64.decode(secretkeyElement.getB64Data());

        try {
            PGPDigestCalculatorProvider calculatorProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build();

            ByteArrayInputStream inputStream = new ByteArrayInputStream(base64Decoded);
            KeyringConfig keyring = KeyringConfigs.withKeyRingsFromStreams(null, inputStream,
                    KeyringConfigCallbacks.withPassword(password));

            Map<OpenPgpV4Fingerprint, PGPSecretKey> availableKeys = new HashMap<>();
            OpenPgpV4Fingerprint selectedKey;

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
                availableKeys.put(BCOpenPgpProvider.getFingerprint(publicKey), secretKey);
            }

            selectedKey = callback.selectSecretKeyToRestore(availableKeys.keySet());
            if (selectedKey != null) {
                try {
                    keyringConfig.addSecretKey(availableKeys.get(selectedKey).getEncoded());
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.INFO, "Users secret key " + selectedKey.toString() + " is already in keyring. Skip.");
                }

                try {
                    keyringConfig.addPublicKey(availableKeys.get(selectedKey).getPublicKey().getEncoded());
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.INFO, "Users public key " + selectedKey.toString() + " is already in keyring. Skip.");
                }
                primaryKeyFingerprint = selectedKey;
                writePrivateKeysToFile(keyringConfig, secretKeyringPath());
            }
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    @Override
    public OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException, SmackOpenPgpException {
        try {
            PGPSecretKeyRing ourKey = BCOpenPgpProvider.generateKey(user).generateSecretKeyRing();
            keyringConfig.addSecretKey(ourKey.getSecretKey().getEncoded());
            keyringConfig.addPublicKey(ourKey.getPublicKey().getEncoded());
            primaryKeyFingerprint = BCOpenPgpProvider.getFingerprint(ourKey.getPublicKey());
            return primaryKeyFingerprint;
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    private File secretKeyringPath() {
        return new File(contactsPath(user), "secring.skr");
    }

    private File publicKeyringPath() {
        return new File(contactsPath(user), "pubring.pkr");
    }

    private File contactsPath() {
        return new File(basePath, user.toString() + "/users");
    }

    private File contactsPath(BareJid contact) {
        return new File(contactsPath(), contact.toString());
    }

    private File contactsList(BareJid contact) {
        return new File(contactsPath(contact), "metadata.list");
    }

    private static void addPublicKeysFromFile(InMemoryKeyring keyring,
                                              File pubring,
                                              KeyringConfigCallback passwordCallback)
            throws IOException, PGPException {
        if (!pubring.exists()) {
            return;
        }

        InputStream inputStream = new FileInputStream(pubring);
        KeyringConfig source = KeyringConfigs.withKeyRingsFromStreams(inputStream, null, passwordCallback);
        for (PGPPublicKeyRing pubRing : source.getPublicKeyRings()) {
            for (PGPPublicKey pubKey : pubRing) {
                try {
                    keyring.addPublicKey(pubKey.getEncoded());
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.INFO, "public key " + Long.toHexString(pubKey.getKeyID()) +
                            " already exists in keyring. Skip.");
                }

            }
        }
    }

    private static PGPPublicKey addSecretKeysFromFile(InMemoryKeyring keyring,
                                                      File secring,
                                                      KeyringConfigCallback passwordCallback)
            throws IOException, PGPException {
        if (!secring.exists()) {
            return null;
        }

        InputStream inputStream = new FileInputStream(secring);
        KeyringConfig source = KeyringConfigs.withKeyRingsFromStreams(null, inputStream, passwordCallback);
        PGPPublicKey lastAdded = null;

        for (PGPSecretKeyRing secRing : source.getSecretKeyRings()) {
            for (PGPSecretKey secKey : secRing) {
                keyring.addSecretKey(secKey.getEncoded());
                // Remember last added secret keys public key -> this will be the primary key
                if (secKey.getPublicKey() != null) {
                    lastAdded = secKey.getPublicKey();
                }
            }
        }

        return lastAdded;
    }

    private static void writePublicKeysToFile(KeyringConfig keyring, File pubring)
            throws IOException, PGPException {
        writeBytesToFile(keyring.getPublicKeyRings().getEncoded(), pubring);
    }

    private static void writePrivateKeysToFile(KeyringConfig keyring, File secring)
            throws IOException, PGPException {
        writeBytesToFile(keyring.getSecretKeyRings().getEncoded(), secring);
    }

    private static void writeBytesToFile(byte[] bytes, File file) throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedStream = new BufferedOutputStream(outputStream);

            bufferedStream.write(bytes);
            outputStream.close();
        } catch (IOException e) {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    @Override
    public KeyringConfig getKeyringConfig() {
        return keyringConfig;
    }
}
