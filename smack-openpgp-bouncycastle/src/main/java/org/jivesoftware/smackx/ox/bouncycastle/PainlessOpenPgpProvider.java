package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
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
import de.vanitasvitae.crypto.pgpainless.decryption_verification.DecryptionStream;
import de.vanitasvitae.crypto.pgpainless.decryption_verification.MissingPublicKeyCallback;
import de.vanitasvitae.crypto.pgpainless.decryption_verification.PainlessResult;
import de.vanitasvitae.crypto.pgpainless.key.generation.type.length.RsaLength;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;

public class PainlessOpenPgpProvider implements OpenPgpProvider {

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

        Set<PGPPublicKeyRing> allRecipientsKeys = getEncryptionKeys(encryptionKeys);
        PGPSecretKeyRing signingKeyRing = getSigningKey(signingKey);

        InputStream fromPlain = element.toInputStream();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        OutputStream encryptor;
        try {
            encryptor = PGPainless.createEncryptor()
                    .onOutputStream(encrypted)
                    .toRecipients((PGPPublicKeyRing[]) allRecipientsKeys.toArray())
                    .usingSecureAlgorithms()
                    .signWith(store.getSecretKeyProtector(), signingKeyRing)
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
    public byte[] sign(SignElement element, OpenPgpV4Fingerprint signingKeyFingerprint)
            throws MissingOpenPgpKeyPairException, IOException, SmackOpenPgpException {
        InputStream fromPlain = element.toInputStream();
        PGPSecretKeyRing signingKeyRing;
        try {
            signingKeyRing = store.getSecretKeyRing(owner).getSecretKeyRing(signingKeyFingerprint.getKeyId());
        } catch (PGPException e) {
            throw new MissingOpenPgpKeyPairException(owner, e);
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
        Set<PGPPublicKeyRing> allRecipientsKeys = getEncryptionKeys(encryptionKeyFingerprints);

        InputStream fromPlain = element.toInputStream();
        ByteArrayOutputStream encrypted = new ByteArrayOutputStream();
        OutputStream encryptor;
        try {
            encryptor = PGPainless.createEncryptor()
                    .onOutputStream(encrypted)
                    .toRecipients((PGPPublicKeyRing[]) allRecipientsKeys.toArray())
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
            throws MissingOpenPgpKeyPairException, SmackOpenPgpException, IOException {
        Set<Long> trustedKeyIds = new HashSet<>();
        Set<PGPPublicKeyRing> senderKeys = new HashSet<>();
        InputStream fromEncrypted = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream toPlain = new ByteArrayOutputStream();
        DecryptionStream decryptionStream;
        try {
            decryptionStream = PGPainless.createDecryptor().onInputStream(fromEncrypted)
                    .decryptWith(store.getSecretKeyRing(owner), store.getSecretKeyProtector())
                    .verifyWith(trustedKeyIds, senderKeys)
                    .handleMissingPublicKeysWith(new MissingPublicKeyCallback() {
                        @Override
                        public void onMissingPublicKeyEncountered(Long aLong) {

                        }
                    })
                    .build();
        } catch (PGPException e) {
            throw new SmackOpenPgpException(e);
        }
        Streams.pipeAll(decryptionStream, toPlain);
        fromEncrypted.close();
        decryptionStream.close();

        PainlessResult result = decryptionStream.getResult();

        return new DecryptedBytesAndMetadata(toPlain.toByteArray(),
                result.getVerifiedSignatureKeyIds(),
                result.getDecryptionKeyId());
    }

    @Override
    public byte[] symmetricallyEncryptWithPassword(byte[] bytes, String password)
            throws SmackOpenPgpException, IOException {
        try {
            return PGPainless.encryptWithPassword(bytes, password.toCharArray());
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

    private Set<PGPPublicKeyRing> getEncryptionKeys(MultiMap<BareJid, OpenPgpV4Fingerprint> encryptionKeys)
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

        return allRecipientsKeys;
    }

    private PGPSecretKeyRing getSigningKey(OpenPgpV4Fingerprint signingKey)
            throws IOException, MissingOpenPgpKeyPairException {
        PGPSecretKeyRing signingKeyRing;
        try {
            signingKeyRing = store.getSecretKeyRing(owner).getSecretKeyRing(signingKey.getKeyId());
        } catch (PGPException e) {
            throw new MissingOpenPgpKeyPairException(owner, e);
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
    public OpenPgpV4Fingerprint importPublicKey(BareJid owner, byte[] bytes) throws MissingUserIdOnKeyException {
        return null;
    }

    @Override
    public OpenPgpV4Fingerprint importSecretKey(BareJid owner, byte[] bytes) throws MissingUserIdOnKeyException {
        return null;
    }

    public static OpenPgpV4Fingerprint getFingerprint(PGPPublicKey publicKey) {
        byte[] hex = Hex.encode(publicKey.getFingerprint());
        return new OpenPgpV4Fingerprint(hex);
    }
}
