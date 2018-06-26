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
package org.jivesoftware.smackx.ox.chat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import org.jxmpp.jid.BareJid;

/**
 * Metadata about a contacts OpenPGP fingerprints.
 */
public class OpenPgpFingerprints {

    private final BareJid jid;
    private final Set<OpenPgpV4Fingerprint> announcedKeys;
    private final Set<OpenPgpV4Fingerprint> availableKeys;
    private final Map<OpenPgpV4Fingerprint, Throwable> unfetchableKeys;

    /**
     * Constructor.
     *
     * @param announcedKeys keys the contact currently announces via their metadata node.
     * @param availableKeys keys which contain the contacts {@link BareJid} as user ID.
     * @param unfetchableKeys keys that are announced, but cannot be fetched fro PubSub.
     */
    public OpenPgpFingerprints(BareJid jid,
                               Set<OpenPgpV4Fingerprint> announcedKeys,
                               Set<OpenPgpV4Fingerprint> availableKeys,
                               Map<OpenPgpV4Fingerprint, Throwable> unfetchableKeys) {
        this.jid = jid;
        this.announcedKeys = Collections.unmodifiableSet(Objects.requireNonNull(announcedKeys,
                "Set of announced keys MUST NOT be null."));
        this.availableKeys = Collections.unmodifiableSet(Objects.requireNonNull(availableKeys,
                "Set of available keys MUST NOT be null."));
        this.unfetchableKeys = Collections.unmodifiableMap(Objects.requireNonNull(unfetchableKeys,
                "Map of unfetchable keys MUST NOT be null."));
    }

    /**
     * Return the {@link BareJid} of the user.
     * @return jid
     */
    public BareJid getJid() {
        return jid;
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s, which the contact in question announced via their
     * metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#announcing-pubkey-list">
     *     XEP-0373 §4.2 about the Public Key Metadata Node</a>
     *
     * @return announced keys.
     */
    public Set<OpenPgpV4Fingerprint> getAnnouncedKeys() {
        return announcedKeys;
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s, which are available in the local key ring, which contain
     * the users {@link BareJid} as user-id (eg. "xmpp:juliet@capulet.lit").
     * <br>
     * Note: As everybody can publish a key with an arbitrary user-id, the keys contained in the result set MAY be keys
     * of an potential attacker. NEVER use these keys for encryption without collating them with the users announced
     * keys.
     *
     * @return locally available keys with users user-id.
     */
    public Set<OpenPgpV4Fingerprint> getAvailableKeys() {
        return availableKeys;
    }

    /**
     * Return a {@link Map}, which maps {@link OpenPgpV4Fingerprint}s which the user announced, but that cannot be fetched
     * via PubSub to {@link Throwable}s which were thrown when we tried to fetch them.
     *
     * @return unfetchable keys.
     */
    public Map<OpenPgpV4Fingerprint, Throwable> getUnfetchableKeys() {
        return unfetchableKeys;
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s, which are both announced, as well as available.
     * It is recommended to use those keys for encryption.
     *
     * @return active keys.
     */
    public Set<OpenPgpV4Fingerprint> getActiveKeys() {
        Set<OpenPgpV4Fingerprint> active = new HashSet<>();
        for (OpenPgpV4Fingerprint fingerprint : getAvailableKeys()) {
            if (getAnnouncedKeys().contains(fingerprint)) {
                active.add(fingerprint);
            }
        }
        return Collections.unmodifiableSet(active);
    }
}
