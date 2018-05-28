package org.jivesoftware.smackx.ox;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Set;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;

import org.jxmpp.jid.BareJid;

public interface OpenPgpStore {

    /**
     * Return the {@link OpenPgpV4Fingerprint} of the primary OpenPGP key pair.
     * If multiple key pairs are available, only the primary key pair is used for signing.
     * <br>
     * Note: This method returns {@code null} if no key pair is available.
     *
     * @return fingerprint of the primary OpenPGP key pair.
     */
    OpenPgpV4Fingerprint primaryOpenPgpKeyPairFingerprint();

    /**
     * Return a {@link Set} containing the {@link OpenPgpV4Fingerprint} of all available OpenPGP key pairs.
     *
     * @return set of fingerprints of available OpenPGP key pairs.
     */
    Set<OpenPgpV4Fingerprint> availableOpenPgpKeyPairFingerprints();

    /**
     * Return a {@link Set} containing the {@link OpenPgpV4Fingerprint}s of all currently announced OpenPGP
     * public keys of a contact.
     * <br>
     * Note: Those are the keys announced in the latest received metadata update.
     * This returns a {@link Set} which might be different from the result of
     * {@link #availableOpenPgpPublicKeysFingerprints(BareJid)}.
     * Messages should be encrypted to the intersection of both sets.
     *
     * @param contact contact.
     * @return list of contacts last announced public keys.
     */
    Set<OpenPgpV4Fingerprint> announcedOpenPgpKeyFingerprints(BareJid contact);

    /**
     * Return a {@link Set} containing the {@link OpenPgpV4Fingerprint}s of all OpenPGP public keys of a
     * contact, which we have locally available.
     * <br>
     * Note: This returns a {@link Set} that might be different from the result of
     * {@link #availableOpenPgpPublicKeysFingerprints(BareJid)}.
     * Messages should be encrypted to the intersection of both sets.
     *
     * @param contact contact.
     * @return list of contacts locally available public keys.
     */
    Set<OpenPgpV4Fingerprint> availableOpenPgpPublicKeysFingerprints(BareJid contact)
            throws CorruptedOpenPgpKeyException;

    /**
     * Store incoming update to the OpenPGP metadata node in persistent storage.
     *
     * @param connection authenticated {@link XMPPConnection} of the user.
     * @param listElement {@link PublicKeysListElement} which contains a list of the keys of {@code owner}.
     * @param owner {@link BareJid} of the owner of the announced public keys.
     */
    void storePublicKeysList(XMPPConnection connection, PublicKeysListElement listElement, BareJid owner);

    /**
     * Create a fresh OpenPGP key pair with the {@link BareJid} of the user prefixed by "xmpp:" as user-id
     * (example: {@code "xmpp:juliet@capulet.lit"}).
     * Store the key pair in persistent storage and return the public keys {@link OpenPgpV4Fingerprint}.
     *
     * @throws NoSuchAlgorithmException if a Hash algorithm is not available
     * @throws NoSuchProviderException id no suitable cryptographic provider (for example BouncyCastleProvider)
     *                                 is registered.
     * @throws CorruptedOpenPgpKeyException if the generated key cannot be added to the keyring for some reason.
     */
    OpenPgpV4Fingerprint createOpenPgpKeyPair()
            throws NoSuchAlgorithmException, NoSuchProviderException, CorruptedOpenPgpKeyException;

    /**
     * Create a {@link PubkeyElement} which contains our exported OpenPGP public key.
     * The element can for example be published.
     *
     * @return {@link PubkeyElement} containing our public key.
     * @throws CorruptedOpenPgpKeyException if our public key can for some reason not be serialized.
     */
    PubkeyElement createPubkeyElement(OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException, CorruptedOpenPgpKeyException;

    /**
     * Process an incoming {@link PubkeyElement} of a contact or ourselves.
     * That typically includes importing/updating the key.
     *
     * @param owner owner of the OpenPGP public key contained in the {@link PubkeyElement}.
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the key.
     * @param element {@link PubkeyElement} which presumably contains the public key of the {@code owner}.
     * @throws CorruptedOpenPgpKeyException if the key found in the {@link PubkeyElement}
     * can not be deserialized or imported.
     */
    void storePublicKey(BareJid owner, OpenPgpV4Fingerprint fingerprint, PubkeyElement element)
            throws CorruptedOpenPgpKeyException;

    /**
     * Create an encrypted backup of our secret keys.
     *
     * @param fingerprints {@link Set} of IDs of the keys that will be included in the backup.
     * @param password password that is used to symmetrically encrypt the backup.
     * @return {@link SigncryptElement}.
     * @throws MissingOpenPgpKeyPairException if we don't have an OpenPGP key available.
     * @throws CorruptedOpenPgpKeyException if for some reason the key pair cannot be serialized.
     */
    SecretkeyElement createSecretkeyElement(Set<OpenPgpV4Fingerprint> fingerprints, String password)
            throws MissingOpenPgpKeyPairException, CorruptedOpenPgpKeyException;

    /**
     * Decrypt a secret key backup and restore the key from it.
     *
     * @param secretkeyElement {@link SecretkeyElement} containing the backup.
     * @param password password to decrypt the backup.
     * @param callback {@link SecretKeyRestoreSelectionCallback} to let the user decide which key to restore.
     * @throws CorruptedOpenPgpKeyException if the selected key is corrupted and cannot be restored.
     * @throws InvalidBackupCodeException if the user provided backup code is invalid.
     */
    void restoreSecretKeyBackup(SecretkeyElement secretkeyElement, String password, SecretKeyRestoreSelectionCallback callback)
            throws CorruptedOpenPgpKeyException, InvalidBackupCodeException;
}
