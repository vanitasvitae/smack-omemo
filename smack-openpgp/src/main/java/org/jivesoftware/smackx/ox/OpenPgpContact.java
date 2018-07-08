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

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.ox.selection_strategy.AnnouncedKeys;
import org.jivesoftware.smackx.ox.selection_strategy.BareJidUserId;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;

public class OpenPgpContact {

    private final Logger LOGGER;

    protected final BareJid jid;
    protected final OpenPgpStore store;

    public OpenPgpContact(BareJid jid, OpenPgpStore store) {
        this.jid = jid;
        this.store = store;
        LOGGER = Logger.getLogger(OpenPgpContact.class.getName() + ":" + jid.toString());
    }

    public BareJid getJid() {
        return jid;
    }

    public PGPPublicKeyRingCollection getAnyPublicKeys() throws IOException, PGPException {
        return store.getPublicKeysOf(jid);
    }

    public PGPPublicKeyRingCollection getAnnouncedPublicKeys() throws IOException, PGPException {
        PGPPublicKeyRingCollection anyKeys = getAnyPublicKeys();
        Map<OpenPgpV4Fingerprint, Date> announced = store.getAnnouncedFingerprintsOf(jid);

        PGPPublicKeyRingCollection announcedKeysCollection = anyKeys;

        BareJidUserId.PubRingSelectionStrategy userIdFilter = new BareJidUserId.PubRingSelectionStrategy();
        AnnouncedKeys.PubKeyRingSelectionStrategy announcedFilter = new AnnouncedKeys.PubKeyRingSelectionStrategy();

        for (PGPPublicKeyRing ring : anyKeys) {

            if (!userIdFilter.accept(getJid(), ring)) {
                LOGGER.log(Level.WARNING, "Ignore key " + Long.toHexString(ring.getPublicKey().getKeyID()) +
                        " as it lacks the user-id \"xmpp" + getJid().toString() + "\"");
                announcedKeysCollection = PGPPublicKeyRingCollection.removePublicKeyRing(announcedKeysCollection, ring);
                continue;
            }

            if (!announcedFilter.accept(announced, ring)) {
                LOGGER.log(Level.WARNING, "Ignore key " + Long.toHexString(ring.getPublicKey().getKeyID()) +
                        " as it is not announced by " + getJid().toString());
                announcedKeysCollection = PGPPublicKeyRingCollection.removePublicKeyRing(announcedKeysCollection, ring);
            }
        }
        return announcedKeysCollection;
    }
}
