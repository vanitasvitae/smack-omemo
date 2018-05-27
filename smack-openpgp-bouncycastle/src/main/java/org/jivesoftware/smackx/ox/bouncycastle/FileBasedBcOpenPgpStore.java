package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpStore;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.Util;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallback;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jxmpp.jid.BareJid;

public class FileBasedBcOpenPgpStore implements OpenPgpStore {

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

        KeyringConfig load = KeyringConfigs.withKeyRingsFromFiles(pub, sec, passwordCallback);
        for (PGPPublicKeyRing pubRing : load.getPublicKeyRings()) {
            for (PGPPublicKey pubKey : pubRing) {
                keyringConfig.addPublicKey(pubKey.getEncoded());
            }
        }

        PGPPublicKey lastAdded = null;

        for (PGPSecretKeyRing secRing : load.getSecretKeyRings()) {
            for (PGPSecretKey secKey : secRing) {
                keyringConfig.addSecretKey(secKey.getEncoded());
                // Remember last added secret keys public key -> this will be the primary key
                if (secKey.getPublicKey() != null) {
                    lastAdded = secKey.getPublicKey();
                }
            }
        }

        if (lastAdded != null) {
            primaryKeyFingerprint = BCOpenPgpProvider.getFingerprint(lastAdded);
        }
    }

    private static void addPublicKeysFromFile(InMemoryKeyring keyring,
                                              File pubring,
                                              KeyringConfigCallback passwordCallback)
            throws IOException, PGPException {
        KeyringConfig source = KeyringConfigs.withKeyRingsFromFiles(pubring, null, passwordCallback);
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
        KeyringConfig source = KeyringConfigs.withKeyRingsFromFiles(null, secring, passwordCallback);
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
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(listPath);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;

                while ((line = bufferedReader.readLine()) != null) {
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
                bufferedReader.close();
            } catch (IOException e) {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e1) {
                        // Ignore
                    }
                }
            }
        }
        return announcedKeys;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeysFingerprints(BareJid contact) {
        Set<OpenPgpV4Fingerprint> availableKeys = new HashSet<>();
        return null; // TODO
    }

    @Override
    public void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner) {
        File listPath = contactsList(owner);
        try {
            if (!listPath.exists()) {
                listPath.getParentFile().mkdirs();
                listPath.createNewFile();
                FileWriter writer = null;
                try {
                    writer = new FileWriter(listPath);
                    BufferedWriter bufferedWriter = new BufferedWriter(writer);

                    for (OpenPgpV4Fingerprint fingerprint : listElement.getMetadata().keySet()) {
                        bufferedWriter.write(fingerprint.toString());
                        bufferedWriter.newLine();
                    }

                    bufferedWriter.close();

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
            throws MissingOpenPgpPublicKeyException, CorruptedOpenPgpKeyException {
        try {
            PGPPublicKey publicKey = keyringConfig.getPublicKeyRings().getPublicKey(Util.keyIdFromFingerprint(fingerprint));
            if (publicKey == null) {
                throw new MissingOpenPgpPublicKeyException(user, fingerprint);
            }
            byte[] base64 = Base64.encode(publicKey.getEncoded());
            return new PubkeyElement(new PubkeyElement.PubkeyDataElement(base64), new Date());
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        }
    }

    @Override
    public void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element)
            throws CorruptedOpenPgpKeyException {
        byte[] base64decoded = Base64.decode(element.getDataElement().getB64Data());
        try {
            keyringConfig.addPublicKey(base64decoded);
        } catch (PGPException | IOException e) {
            throw new CorruptedOpenPgpKeyException(e);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Public Key with ID " + fingerprint.toString() + " of " +
                    owner + " is already in memory. Skip.");
        }
    }

    @Override
    public SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password)
            throws MissingOpenPgpKeyPairException, CorruptedOpenPgpKeyException {
        return null;
    }

    @Override
    public void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws CorruptedOpenPgpKeyException, InvalidBackupCodeException {

    }

    private File secretKeyringPath() {
        return new File(contactsPath(user), "secring.skr");
    }

    private File publicKeyringPath() {
        return publicKeyringPath(user);
    }

    private File publicKeyringPath(BareJid contact) {
        return new File(contactsPath(contact), "pubring.pkr");
    }

    private File contactsPath() {
        return new File(basePath, "users");
    }

    private File contactsPath(BareJid contact) {
        return new File(contactsPath(), contact.toString());
    }

    private File contactsList(BareJid contact) {
        return new File(contactsPath(contact), "metadata.list");
    }
}
