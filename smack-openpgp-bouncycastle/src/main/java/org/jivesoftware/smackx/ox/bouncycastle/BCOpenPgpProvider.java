package org.jivesoftware.smackx.ox.bouncycastle;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Set;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.jxmpp.jid.BareJid;

public class BCOpenPgpProvider implements OpenPgpProvider {

    private final BareJid user;
    private OpenPgpV4Fingerprint primaryKeyPair;


    public BCOpenPgpProvider(BareJid user) {
        this.user = user;
        this.primaryKeyPair = null;
    }

    @Override
    public OpenPgpV4Fingerprint primaryOpenPgpKeyPairFingerprint() {
        return primaryKeyPair;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeyPairFingerprints() {
        return null;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> announcedOpenPgpKeyFingerprints(BareJid contact) {
        return null;
    }

    @Override
    public OpenPgpElement signAndEncrypt(SigncryptElement element, OpenPgpV4Fingerprint signingKey, Set<OpenPgpV4Fingerprint> encryptionKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {
        return null;
    }

    @Override
    public OpenPgpMessage decryptAndVerify(OpenPgpElement element, Set<OpenPgpV4Fingerprint> sendersKeys)
            throws MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {
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

    @Override
    public PubkeyElement createPubkeyElement(OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException, CorruptedOpenPgpKeyException {
        return null;
    }

    @Override
    public void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element)
            throws CorruptedOpenPgpKeyException {

    }

    @Override
    public void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner)
            throws CorruptedOpenPgpKeyException, InterruptedException, SmackException.NotConnectedException,
            SmackException.NoResponseException {

    }

    @Override
    public SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password)
            throws MissingOpenPgpKeyPairException, CorruptedOpenPgpKeyException {
        return null;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> availableOpenPgpKeysFingerprints(BareJid contact) {
        return null;
    }

    @Override
    public void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws CorruptedOpenPgpKeyException, InvalidBackupCodeException {

    }

    @Override
    public OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException {
        return null;
    }

    public static OpenPgpV4Fingerprint getFingerprint(PGPPublicKey publicKey) {
        byte[] hex = Hex.encode(publicKey.getFingerprint());
        return new OpenPgpV4Fingerprint(hex);
    }
}
