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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.bouncycastle.selection_strategy.BareJidUserId;
import org.jivesoftware.smackx.ox.callback.SmackMissingOpenPgpPublicKeyCallback;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.DecryptedBytesAndMetadata;
import org.jivesoftware.smackx.ox.util.KeyBytesAndFingerprint;

import de.vanitasvitae.crypto.pgpainless.PGPainless;
import de.vanitasvitae.crypto.pgpainless.algorithm.SymmetricKeyAlgorithm;
import de.vanitasvitae.crypto.pgpainless.decryption_verification.DecryptionStream;
import de.vanitasvitae.crypto.pgpainless.decryption_verification.PainlessResult;
import de.vanitasvitae.crypto.pgpainless.key.SecretKeyRingProtector;
import de.vanitasvitae.crypto.pgpainless.key.generation.type.length.RsaLength;
import de.vanitasvitae.crypto.pgpainless.util.BCUtil;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;

public class PainlessOpenPgpProvider implements OpenPgpProvider {

    private static final Logger LOGGER = Logger.getLogger(PainlessOpenPgpProvider.class.getName());

    private final PainlessOpenPgpStore store;
    private final BareJid owner;

    public PainlessOpenPgpProvider(BareJid owner, PainlessOpenPgpStore store) {
        this.owner = owner;
        this.store = store;
    }

    @Override
    public byte[] signAndEncrypt(SigncryptElement element,
                                 OpenPgpV4Fingerprint signingKey,
                                 MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException, SmackOpenPgpException,
            IOException {

        PGPSecretKeyRing secretKeyRing;
        SecretKeyRingProtector protector = getStore().getSecretKeyProtector();

        try {
            secretKeyRing = getStore()
                    .getSecretKeyRings(owner)
                    .getSecretKeyRing(
                            signingKey.getKeyId());
        } catch (PGPException e) {
            LOGGER.log(Level.INFO, "Could not get secret key with id " + Long.toHexString(signingKey.getKeyId()), e);
            throw new MissingOpenPgpKeyPairException(owner, signingKey, e);
        }

        MultiMap<BareJid, PGPPublicKeyRing> publicKeyRingMultiMap = new MultiMap<>();
        for (BareJid jid : encryptionKeys.keySet()) {
            try {
                PGPPublicKeyRingCollection publicKeyRings = getStore().getPublicKeyRings(jid);
                for (OpenPgpV4Fingerprint f : encryptionKeys.getAll(jid)) {
                    PGPPublicKeyRing ring = publicKeyRings.getPublicKeyRing(f.getKeyId());
                    if (ring != null) {
                        publicKeyRingMultiMap.put(jid, ring);
                    }
                }
            } catch (PGPException e) {
                LOGGER.log(Level.INFO, "Could get public keys of " + jid.toString());
                throw new MissingOpenPgpPublicKeyException(owner, encryptionKeys.getFirst(jid));
            }
        }

        return signAndEncryptImpl(element, secretKeyRing, protector, publicKeyRingMultiMap);
    }

    byte[] signAndEncryptImpl(SigncryptElement element,
                              PGPSecretKeyRing signingKey,
                              SecretKeyRingProtector secretKeyRingProtector,
                              MultiMap<BareJid, PGPPublicKeyRing> encryptionKeys)
            throws SmackOpenPgpException, IOException {
        InputStream fromPlain = element.toInputStream();
        ByteArrayOutputStream encryptedBytes = new ByteArrayOutputStream();

        OutputStream toEncrypted;
        try {
            toEncrypted = PGPainless.createEncryptor()
                    .onOutputStream(encryptedBytes)
                    .toRecipients(new ArrayList<>(encryptionKeys.values()).toArray(new PGPPublicKeyRing[] {}))
                    .usingSecureAlgorithms()
                    .signWith(secretKeyRingProtector, signingKey)
                    .noArmor();
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException(e);
        }

        Streams.pipeAll(fromPlain, toEncrypted);
        toEncrypted.flush();
        toEncrypted.close();

        encryptedBytes.close();

        return encryptedBytes.toByteArray();
    }

    @Override
    public byte[] sign(SignElement element, OpenPgpV4Fingerprint signingKeyFingerprint)
            throws MissingOpenPgpKeyPairException, IOException, SmackOpenPgpException {
        InputStream fromPlain = element.toInputStream();
        PGPSecretKeyRing signingKeyRing;
        try {
            signingKeyRing = store.getSecretKeyRings(owner).getSecretKeyRing(signingKeyFingerprint.getKeyId());
        } catch (PGPException e) {
            throw new MissingOpenPgpKeyPairException(owner, signingKeyFingerprint, e);
        }

        ByteArrayOutputStream toSigned = new ByteArrayOutputStream();
        OutputStream signer;
        try {
            signer = PGPainless.createEncryptor().onOutputStream(toSigned)
                    .doNotEncrypt()
                    .signWith(store.getSecretKeyProtector(), signingKeyRing)
                    .noArmor();
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }

        Streams.pipeAll(fromPlain, signer);

        fromPlain.close();
        signer.close();

        return toSigned.toByteArray();
    }

    @Override
    public byte[] encrypt(CryptElement element, MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeyFingerprints)
            throws MissingOpenPgpPublicKeyException, IOException, SmackOpenPgpException {
        PGPPublicKeyRing[] allRecipientsKeys = getEncryptionKeys(encryptionKeyFingerprints);

        InputStream fromPlain = element.toInputStream();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        OutputStream encryptor;
        try {
            encryptor = PGPainless.createEncryptor()
                    .onOutputStream(encrypted)
                    .toRecipients(allRecipientsKeys)
                    .usingSecureAlgorithms()
                    .doNotSign()
                    .noArmor();
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }

        Streams.pipeAll(fromPlain, encryptor);
        fromPlain.close();
        encryptor.close();

        return encrypted.toByteArray();
    }

    @Override
    public DecryptedBytesAndMetadata decrypt(byte[] bytes, BareJid sender, final SmackMissingOpenPgpPublicKeyCallback missingPublicKeyCallback)
            throws MissingOpenPgpKeyPairException, SmackOpenPgpException {

        PGPSecretKeyRingCollection secretKeyRings;
        try {
            secretKeyRings = getStore().getSecretKeyRings(owner);
        } catch (PGPException | IOException e) {
            LOGGER.log(Level.INFO, "Could not get secret keys of user " + owner);
            throw new MissingOpenPgpKeyPairException(owner, getStore().getPrimaryOpenPgpKeyPairFingerprint());
        }

        SecretKeyRingProtector protector = getStore().getSecretKeyProtector();

        List<OpenPgpV4Fingerprint> trustedFingerprints = getStore().getAllContactsTrustedFingerprints().getAll(sender);
        Set<Long> trustedKeyIds = new HashSet<>();
        for (OpenPgpV4Fingerprint fingerprint : trustedFingerprints) {
            trustedKeyIds.add(fingerprint.getKeyId());
        }

        PGPPublicKeyRingCollection publicKeyRings;
        try {
            publicKeyRings = getStore().getPublicKeyRings(sender);
        } catch (PGPException | IOException e) {
            LOGGER.log(Level.INFO, "Could not get public keys of sender " + sender.toString(), e);
            if (missingPublicKeyCallback != null) {
                // TODO: Handle missing key
            }
            throw new SmackOpenPgpException(e);
        }

        Iterator<PGPPublicKeyRing> iterator = publicKeyRings.getKeyRings();
        Set<PGPPublicKeyRing> trustedKeys = new HashSet<>();
        while (iterator.hasNext()) {
            PGPPublicKeyRing ring = iterator.next();
            if (trustedKeyIds.contains(ring.getPublicKey().getKeyID())) {
                trustedKeys.add(ring);
            }
        }

        try {
            return decryptImpl(bytes, secretKeyRings, protector, trustedKeys);
        } catch (IOException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    DecryptedBytesAndMetadata decryptImpl(byte[] bytes, PGPSecretKeyRingCollection decryptionKeys,
                                          SecretKeyRingProtector protector,
                                          Set<PGPPublicKeyRing> verificationKeys)
            throws SmackOpenPgpException, IOException {

        InputStream encryptedBytes = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream toPlain = new ByteArrayOutputStream();
        DecryptionStream fromEncrypted;
        try {
            fromEncrypted = PGPainless.createDecryptor()
                    .onInputStream(encryptedBytes)
                    .decryptWith(decryptionKeys, protector)
                    .verifyWith(verificationKeys)
                    .ignoreMissingPublicKeys()
                    .build();
        } catch (IOException | PGPException e) {
            throw new SmackOpenPgpException(e);
        }

        Streams.pipeAll(fromEncrypted, toPlain);

        fromEncrypted.close();
        toPlain.flush();
        toPlain.close();

        PainlessResult result = fromEncrypted.getResult();
        return new DecryptedBytesAndMetadata(toPlain.toByteArray(),
                result.getVerifiedSignatureKeyIds(),
                result.getDecryptionKeyId());
    }

    @Override
    public byte[] symmetricallyEncryptWithPassword(byte[] bytes, String password)
            throws SmackOpenPgpException, IOException {
        try {
            return PGPainless.encryptWithPassword(bytes, password.toCharArray(), SymmetricKeyAlgorithm.AES_256);
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    @Override
    public byte[] symmetricallyDecryptWithPassword(byte[] bytes, String password)
            throws SmackOpenPgpException, IOException {
        try {
            return PGPainless.decryptWithPassword(bytes, password.toCharArray());
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }
    }

    @Override
    public PainlessOpenPgpStore getStore() {
        return store;
    }

    private PGPPublicKeyRing[] getEncryptionKeys(MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeys)
            throws IOException, SmackOpenPgpException {
        Set<PGPPublicKeyRing> allRecipientsKeys = new HashSet<>();

        for (BareJid recipient : encryptionKeys.keySet()) {
            PGPPublicKeyRingCollection recipientsKeyRings;
            try {
                recipientsKeyRings = store.getPublicKeyRings(recipient);
                for (OpenPgpV4Fingerprint fingerprint : encryptionKeys.getAll(recipient)) {
                    PGPPublicKeyRing ring = recipientsKeyRings.getPublicKeyRing(fingerprint.getKeyId());
                    if (ring != null) allRecipientsKeys.add(ring);
                }
            } catch (PGPException e) {
                throw new SmackOpenPgpException(e);
            }
        }

        PGPPublicKeyRing[] allEncryptionKeys = new PGPPublicKeyRing[allRecipientsKeys.size()];
        Iterator<PGPPublicKeyRing> iterator = allRecipientsKeys.iterator();
        for (int i = 0; i < allEncryptionKeys.length; i++) {
            allEncryptionKeys[i] = iterator.next();
        }

        return allEncryptionKeys;
    }

    private PGPSecretKeyRing getSigningKey(OpenPgpV4Fingerprint signingKey)
            throws IOException, MissingOpenPgpKeyPairException {
        PGPSecretKeyRing signingKeyRing;
        try {
            signingKeyRing = store.getSecretKeyRings(owner).getSecretKeyRing(signingKey.getKeyId());
        } catch (PGPException e) {
            throw new MissingOpenPgpKeyPairException(owner, signingKey, e);
        }
        return signingKeyRing;
    }

    @Override
    public KeyBytesAndFingerprint generateOpenPgpKeyPair(BareJid owner)
            throws SmackOpenPgpException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        PGPSecretKeyRing secretKey;
        try {
            secretKey = PGPainless.generateKeyRing().simpleRsaKeyRing("xmpp:" + owner.toString(), RsaLength._4096);
        } catch (PGPException e) {
            throw new SmackOpenPgpException("Could not generate OpenPGP Key Pair.", e);
        }

        return new KeyBytesAndFingerprint(secretKey.getEncoded(), getFingerprint(secretKey.getPublicKey()));
    }

    @Override
    public OpenPgpV4Fingerprint importPublicKey(BareJid owner, byte[] bytes)
            throws MissingUserIdOnKeyException, IOException, SmackOpenPgpException {
        PGPPublicKeyRing publicKeys = new PGPPublicKeyRing(bytes, new BcKeyFingerprintCalculator());
        return importPublicKey(owner, publicKeys);
    }

    public OpenPgpV4Fingerprint importPublicKey(BareJid owner, PGPPublicKeyRing ring)
            throws SmackOpenPgpException, IOException, MissingUserIdOnKeyException {
        if (!new BareJidUserId.PubRingSelectionStrategy().accept(owner, ring)) {
            throw new MissingUserIdOnKeyException(owner, ring.getPublicKey().getKeyID());
        }
        try {
            PGPPublicKeyRingCollection publicKeyRings = getStore().getPublicKeyRings(owner);
            if (publicKeyRings == null) {
                publicKeyRings = new PGPPublicKeyRingCollection(Collections.singleton(ring));
            } else {
                publicKeyRings = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRings, ring);
            }
            getStore().storePublicKeyRing(owner, publicKeyRings);
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }
        return getFingerprint(ring.getPublicKey());
    }

    @Override
    public OpenPgpV4Fingerprint importSecretKey(BareJid owner, byte[] bytes)
            throws MissingUserIdOnKeyException, SmackOpenPgpException, IOException {
        PGPSecretKeyRing importSecretKeys;
        try {
            importSecretKeys = new PGPSecretKeyRing(bytes, new BcKeyFingerprintCalculator());
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException("Could not deserialize PGP secret key of " + owner.toString(), e);
        }

        store.setPrimaryOpenPgpKeyPairFingerprint(getFingerprint(importSecretKeys.getPublicKey()));

        if (!new BareJidUserId.SecRingSelectionStrategy().accept(owner, importSecretKeys)) {
            throw new MissingUserIdOnKeyException(owner, importSecretKeys.getPublicKey().getKeyID());
        }

        PGPSecretKeyRingCollection secretKeyRings;
        try {
            secretKeyRings = getStore().getSecretKeyRings(owner);
        } catch (PGPException | IOException e) {
            throw new SmackOpenPgpException("Could not load secret key ring of " + owner.toString(), e);
        }
        if (secretKeyRings == null) {
            try {
                secretKeyRings = new PGPSecretKeyRingCollection(Collections.singleton(importSecretKeys));
            } catch (IOException | PGPException e) {
                throw new SmackOpenPgpException("Could not create SecretKeyRingCollection from SecretKeyRing.", e);
            }
        } else {
            try {
                secretKeyRings = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRings, importSecretKeys);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.INFO, "Skip key " + Long.toHexString(importSecretKeys.getPublicKey().getKeyID()) +
                        " as it is already part of the key ring.");
            }
        }
        getStore().storeSecretKeyRing(owner, secretKeyRings);

        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(importSecretKeys);
        importPublicKey(owner, publicKeys);

        return getFingerprint(publicKeys.getPublicKey());
    }

    public static OpenPgpV4Fingerprint getFingerprint(PGPPublicKey publicKey) {
        byte[] hex = Hex.encode(publicKey.getFingerprint());
        return new OpenPgpV4Fingerprint(hex);
    }
}
