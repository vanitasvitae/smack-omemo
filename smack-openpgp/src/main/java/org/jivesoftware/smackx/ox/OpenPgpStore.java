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
package org.jivesoftware.smackx.ox;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

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
    OpenPgpV4Fingerprint getPrimaryOpenPgpKeyPairFingerprint();

    /**
     * Set the {@link OpenPgpV4Fingerprint} of the primary OpenPGP key pair.
     * If multiple key pairs are available, only the primary key pair is used for signing.
     *
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the new primary key pair.
     */
    void setPrimaryOpenPgpKeyPairFingerprint(OpenPgpV4Fingerprint fingerprint);

    /**
     * Return a {@link Set} containing the {@link OpenPgpV4Fingerprint}s of the master keys of all available
     * OpenPGP key pairs of {@code owner}.
     *
     * @param owner owner.
     * @return set of fingerprints of available OpenPGP key pairs master keys.
     */
    Set<OpenPgpV4Fingerprint> getAvailableKeyPairFingerprints(BareJid owner) throws SmackOpenPgpException;

    /**
     * Return a {@link Map} containing the {@link OpenPgpV4Fingerprint}s of all OpenPGP public keys of a
     * contact, which we have locally available, as well as the date, those keys had been published on.
     * <br>
     * Note: This returns a {@link Map} that might be different from the result of
     * {@link #getAvailableKeysFingerprints(BareJid)} (BareJid)}.
     * Messages should be encrypted to the intersection of both key sets.
     *
     * @param contact contact.
     * @return list of contacts locally available public keys.
     *
     * @throws SmackOpenPgpException if something goes wrong
     */
    Map<OpenPgpV4Fingerprint, Date> getAvailableKeysFingerprints(BareJid contact)
            throws SmackOpenPgpException;

    /**
     * Return a {@link Map} containing the {@link OpenPgpV4Fingerprint}s of all currently announced OpenPGP
     * public keys of a contact along with the dates of their latest revision.
     * <br>
     * Note: Those are the keys announced in the latest received metadata update.
     * This returns a {@link Map} which might contain different {@link OpenPgpV4Fingerprint}s than the result of
     * {@link #getAvailableKeysFingerprints(BareJid)} (BareJid)}.
     * Messages should be encrypted to the intersection of both key sets.
     *
     * @param contact contact.
     * @return map of contacts last announced public keys and their update dates.
     */
    Map<OpenPgpV4Fingerprint, Date> getAnnouncedKeysFingerprints(BareJid contact);

    /**
     * Store a {@link Map} of a contacts fingerprints and publication dates in persistent storage.
     *
     * @param contact {@link BareJid} of the owner of the announced public keys.
     * @param fingerprints {@link Map} which contains a list of the keys of {@code owner}.
     */
    void setAnnouncedKeysFingerprints(BareJid contact, Map<OpenPgpV4Fingerprint, Date> fingerprints);

    /**
     * Return the a {@link Map} of {@link OpenPgpV4Fingerprint}s and the {@link Date}s of when they were last
     * fetched from PubSub.
     *
     * @param owner owner of the keys
     * @return {@link Map} of keys last revision dates.
     */
    Map<OpenPgpV4Fingerprint, Date> getPubkeysLastRevisions(BareJid owner);

    /**
     * Set the last revision dates of all keys of a contact.
     *
     * @param owner owner of the keys
     * @param revisionDates {@link Map} of {@link OpenPgpV4Fingerprint}s and the {@link Date}s of when they
     *                                 were last fetched from PubSub.
     */
    void setPubkeysLastRevision(BareJid owner, Map<OpenPgpV4Fingerprint, Date> revisionDates);

    /**
     * Return a {@link MultiMap} which contains contacts and their trusted keys {@link OpenPgpV4Fingerprint}s.
     *
     * @return trusted fingerprints.
     */
    MultiMap<BareJid, OpenPgpV4Fingerprint> getAllContactsTrustedFingerprints();

    /**
     * Return the byte array representation of {@code owner}s public key ring with fingerprint {@code fingerprint}.
     *
     * @param owner owner of the key
     * @param fingerprint fingerprint of the key
     * @return byte representation of the public key.
     */
    byte[] getPublicKeyRingBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException;

    /**
     * Return the byte array representation of {@code owner}s secret key ring with fingerprint {@code fingerprint}.
     *
     * @param owner owner of the key
     * @param fingerprint fingerprint of the key
     * @return byte representation of the secret key.
     */
    byte[] getSecretKeyRingBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpKeyPairException;

}
