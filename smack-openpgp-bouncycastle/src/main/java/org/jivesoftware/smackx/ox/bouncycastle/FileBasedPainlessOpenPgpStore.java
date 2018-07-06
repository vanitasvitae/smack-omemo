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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppDateTime;
import org.pgpainless.pgpainless.key.protection.SecretKeyRingProtector;

public class FileBasedPainlessOpenPgpStore extends AbstractPainlessOpenPgpStore {

    private static final Logger LOGGER = Logger.getLogger(FileBasedPainlessOpenPgpStore.class.getName());

    private final File basePath;

    public FileBasedPainlessOpenPgpStore(File base, SecretKeyRingProtector secretKeyRingProtector) {
        super(secretKeyRingProtector);
        this.basePath = base;
    }

    @Override
    public byte[] loadPublicKeyRingBytes(BareJid owner) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FileInputStream inputStream = null;
        byte[] bytes = null;
        try {
            inputStream = new FileInputStream(getContactsPubringFile(owner, false));
            Streams.pipeAll(inputStream, buffer);
            inputStream.close();
            bytes = buffer.toByteArray();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "Pubring of user " + owner.toString() + " does not exist.");
            return null;
        } catch (IOException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ee) {
                    LOGGER.log(Level.WARNING, "Could not close InputStream:", ee);
                }
            }
        }

        return bytes;
    }

    @Override
    public byte[] loadSecretKeyRingBytes(BareJid owner) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        FileInputStream inputStream = null;
        byte[] bytes = null;
        try {
            inputStream = new FileInputStream(getContactsSecringFile(owner, false));
            Streams.pipeAll(inputStream, buffer);
            inputStream.close();
            bytes = buffer.toByteArray();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "Secring of user " + owner.toString() + " does not exist.");
            return null;
        } catch (IOException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ee) {
                    LOGGER.log(Level.WARNING, "Could not close InputStream:", ee);
                }
            }
        }

        return bytes;
    }

    @Override
    public void storePublicKeyRingBytes(BareJid owner, byte[] bytes) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(getContactsPubringFile(owner, true));
            outputStream.write(bytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            throw new AssertionError("File does not exist, even though it MUST exist at this point.", e);
        } catch (IOException e) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ee) {
                    LOGGER.log(Level.WARNING, "Could not close OutputStream:", ee);
                }
            }
        }
    }

    @Override
    public void storeSecretKeyRingBytes(BareJid owner, byte[] bytes) {
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(getContactsSecringFile(owner, true));
            outputStream.write(bytes);
            outputStream.close();
        } catch (FileNotFoundException e) {
            throw new AssertionError("File does not exist, even though it MUST exist at this point.", e);
        } catch (IOException e) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ee) {
                    LOGGER.log(Level.WARNING, "Could not close OutputStream:", ee);
                }
            }
        }
    }

    @Override
    public Set<OpenPgpV4Fingerprint> getAvailableKeyPairFingerprints(BareJid owner) throws SmackOpenPgpException {
        Set<OpenPgpV4Fingerprint> fingerprints = new HashSet<>();
        try {
            PGPSecretKeyRingCollection secretKeys = getSecretKeyRings(owner);
            for (PGPSecretKeyRing s : secretKeys != null ? secretKeys : Collections.<PGPSecretKeyRing>emptySet()) {
                fingerprints.add(PainlessOpenPgpProvider.getFingerprint(s.getPublicKey()));
            }
        } catch (IOException | PGPException e) {
            throw new SmackOpenPgpException("Could not get available key pair fingerprints.", e);
        }

        return fingerprints;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAvailableKeysFingerprints(BareJid contact) throws SmackOpenPgpException {
        Map<OpenPgpV4Fingerprint, Date> availableFingerprints = new HashMap<>();
        try {
            PGPPublicKeyRingCollection publicKeys = getPublicKeyRings(contact);
            Set<OpenPgpV4Fingerprint> fingerprints = new HashSet<>();
            for (PGPPublicKeyRing ring : publicKeys != null ? publicKeys : Collections.<PGPPublicKeyRing>emptySet()) {
                OpenPgpV4Fingerprint fingerprint = PainlessOpenPgpProvider.getFingerprint(ring.getPublicKey());
                fingerprints.add(fingerprint);
            }

            Map<OpenPgpV4Fingerprint, Date> announced = getAnnouncedKeysFingerprints(contact);
            for (OpenPgpV4Fingerprint fingerprint : fingerprints) {
                if (announced.containsKey(fingerprint)) {
                    availableFingerprints.put(fingerprint, announced.get(fingerprint));
                }
            }

        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException("Could not read public keys of contact " + contact.toString(), e);
        }
        return availableFingerprints;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAnnouncedKeysFingerprints(BareJid contact) {
        try {
            File file = getContactsPubkeyAnnouncementFile(contact, false);
            return loadFingerprintsAndDates(file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read announced fingerprints of contact " + contact, e);
        }
        return new HashMap<>();
    }

    @Override
    public void setAnnouncedKeysFingerprints(BareJid contact, Map<OpenPgpV4Fingerprint, Date> fingerprints) {
        try {
            File file = getContactsPubkeyAnnouncementFile(contact, true);
            writeFingerprintsAndDates(fingerprints, file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not write announced fingerprints of " + contact.toString(), e);
        }
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getPubkeysLastRevisions(BareJid owner) {
        try {
            return loadFingerprintsAndDates(getContactsPubkeyRevisionInfoFile(owner, false));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read revision dates of pubkeys of " + owner.toString(), e);
            return new HashMap<>();
        }
    }

    private static Map<OpenPgpV4Fingerprint, Date> loadFingerprintsAndDates(File file) throws IOException {
        Map<OpenPgpV4Fingerprint, Date> revisionDates = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(file.toPath(), Charset.forName("UTF-8"));
            int lineNr = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNr++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] split = line.split(" ");
                if (split.length != 2) {
                    continue;
                }

                try {
                    OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(split[0]);
                    Date date = XmppDateTime.parseXEP0082Date(split[1]);
                    revisionDates.put(fingerprint, date);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Encountered illegal line in file " +
                            file.getAbsolutePath() + ": " + lineNr, e);
                }
            }
            reader.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read fingerprints and dates from file " + file.getAbsolutePath(), e);
            if (reader != null) {
                reader.close();
            }
        }
        return revisionDates;
    }

    private static void writeFingerprintsAndDates(Map<OpenPgpV4Fingerprint, Date> fingerprints, File file) throws IOException {
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"));
            for (OpenPgpV4Fingerprint fingerprint : fingerprints.keySet()) {
                Date date = fingerprints.get(fingerprint);
                String line = fingerprint.toString() + " " + (date != null ? XmppDateTime.formatXEP0082Date(date) : XmppDateTime.formatXEP0082Date(new Date()));
                writer.write(line);
            }
            writer.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not write fingerprints and dates to file " + file.getAbsolutePath());
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public void setPubkeysLastRevision(BareJid owner, Map<OpenPgpV4Fingerprint, Date> revisionDates) {
        try {
            File file = getContactsPubkeyRevisionInfoFile(owner, true);
            writeFingerprintsAndDates(revisionDates, file);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not write last pubkey revisions of " + owner, e);
        }
    }

    @Override
    public MultiMap<BareJid, OpenPgpV4Fingerprint> getAllContactsTrustedFingerprints() {
        MultiMap<BareJid, OpenPgpV4Fingerprint> trustedFingerprints = new MultiMap<>();
        try {
            File contactsDir = getContactsPath(false);
            if (!contactsDir.exists()) {
                LOGGER.log(Level.FINE, "Contacts directory does not exists yet.");
                return trustedFingerprints;
            }

            File[] subDirectories = contactsDir.listFiles(directoryFilter);
            if (subDirectories == null) {
                return trustedFingerprints;
            }

            for (File contact : subDirectories) {
                BareJid jid = JidCreate.bareFrom(contact.getName());
                try {
                    PGPPublicKeyRingCollection publicKeyRings = getPublicKeyRings(jid);
                    for (PGPPublicKeyRing ring : publicKeyRings) {
                        OpenPgpV4Fingerprint fingerprint = PainlessOpenPgpProvider.getFingerprint(ring.getPublicKey());
                        trustedFingerprints.put(jid, fingerprint);
                    }
                } catch (PGPException e) {
                    LOGGER.log(Level.WARNING, "Could not read public key ring of " + jid, e);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not read contacts directory", e);
        }
        return trustedFingerprints;
    }

    @Override
    public byte[] getPublicKeyRingBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException {
        try {
            PGPPublicKeyRingCollection publicKeyRings = getPublicKeyRings(owner);
            PGPPublicKeyRing ring = publicKeyRings.getPublicKeyRing(fingerprint.getKeyId());
            if (ring != null) {
                return ring.getEncoded(true);
            } else {
                throw new MissingOpenPgpPublicKeyException(owner, fingerprint);
            }
        } catch (IOException | PGPException e) {
            throw new MissingOpenPgpPublicKeyException(owner, fingerprint, e);
        }
    }

    @Override
    public byte[] getSecretKeyRingBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpKeyPairException {
        try {
            PGPSecretKeyRingCollection secretKeyRings = getSecretKeyRings(owner);
            PGPSecretKeyRing ring = secretKeyRings.getSecretKeyRing(fingerprint.getKeyId());
            if (ring != null) {
                return ring.getEncoded();
            } else {
                throw new MissingOpenPgpKeyPairException(owner, fingerprint);
            }
        } catch (IOException | PGPException e) {
            throw new MissingOpenPgpKeyPairException(owner, fingerprint, e);
        }
    }

    /*
    ####################################################################################################################
                                            File System Hierarchy
    ####################################################################################################################
     */

    private File getStorePath(boolean create)
            throws IOException {
        if (create && !basePath.exists()) {
            createDirectoryOrThrow(basePath);
        }
        return basePath;
    }

    private File getContactsPath(boolean create) throws IOException {
        File path = new File(getStorePath(create), "contacts");
        if (create && !path.exists()) {
            createDirectoryOrThrow(path);
        }
        return path;
    }

    private File getContactsPath(BareJid owner, boolean create) throws IOException {
        File path = new File(getContactsPath(create), owner.toString());
        if (create && !path.exists()) {
            createDirectoryOrThrow(path);
        }
        return path;
    }

    private File getContactsPubringFile(BareJid owner, boolean create) throws IOException {
        File file = new File(getContactsPath(owner, create), "pubring.pkr");
        if (create && !file.exists()) {
            createFileOrThrow(file);
        }
        return file;
    }

    private File getContactsSecringFile(BareJid owner, boolean create) throws IOException {
        File file = new File(getContactsPath(owner, create), "secring.skr");
        if (create && !file.exists()) {
            createFileOrThrow(file);
        }
        return file;
    }

    private File getContactsPubkeyRevisionInfoFile(BareJid owner, boolean create) throws IOException {
        File file = new File(getContactsPath(owner, create), "revisionDates.lst");
        if (create && !file.exists()) {
            createFileOrThrow(file);
        }
        return file;
    }

    private File getContactsPubkeyAnnouncementFile(BareJid owner, boolean create) throws IOException {
        File file = new File(getContactsPath(owner, create), "announcedKeys.lst");
        if (create && !file.exists()) {
            createFileOrThrow(file);
        }
        return file;
    }

    private static void createDirectoryOrThrow(File dir) throws IOException {
        if (!dir.mkdirs()) {
            throw new IOException("Could not create directory \"" + dir.getAbsolutePath() + "\"");
        }
    }

    private static void createFileOrThrow(File file) throws IOException {
        if (!file.createNewFile()) {
            throw new IOException("Could not create file \"" + file.getAbsolutePath() + "\"");
        }
    }

    private static final FileFilter directoryFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file != null && file.isDirectory();
        }
    };
}
