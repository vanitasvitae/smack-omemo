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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.selection_strategy.BareJidUserId;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;
import org.jivesoftware.smackx.pubsub.PubSubException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.util.BCUtil;

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

        BareJidUserId.PubRingSelectionStrategy userIdFilter = new BareJidUserId.PubRingSelectionStrategy();

        PGPPublicKeyRingCollection announcedKeysCollection = null;
        for (OpenPgpV4Fingerprint announcedFingerprint : announced.keySet()) {
            PGPPublicKeyRing ring = anyKeys.getPublicKeyRing(announcedFingerprint.getKeyId());

            if (ring == null) continue;

            ring = BCUtil.removeUnassociatedKeysFromKeyRing(ring, ring.getPublicKey(announcedFingerprint.getKeyId()));

            if (!userIdFilter.accept(getJid(), ring)) {
                LOGGER.log(Level.WARNING, "Ignore key " + Long.toHexString(ring.getPublicKey().getKeyID()) +
                        " as it lacks the user-id \"xmpp" + getJid().toString() + "\"");
                continue;
            }

            if (announcedKeysCollection == null) {
                announcedKeysCollection = new PGPPublicKeyRingCollection(Collections.singleton(ring));
            } else {
                announcedKeysCollection = PGPPublicKeyRingCollection.addPublicKeyRing(announcedKeysCollection, ring);
            }
        }

        return announcedKeysCollection;
    }

    public void updateKeys(XMPPConnection connection) throws InterruptedException, SmackException.NotConnectedException,
            SmackException.NoResponseException, XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException,
            PubSubException.NotAPubSubNodeException, IOException {
        PublicKeysListElement metadata = PubSubDelegate.fetchPubkeysList(connection, getJid());
        if (metadata == null) {
            return;
        }

        Map<OpenPgpV4Fingerprint, Date> fingerprintsAndDates = new HashMap<>();
        for (OpenPgpV4Fingerprint fingerprint : metadata.getMetadata().keySet()) {
            fingerprintsAndDates.put(fingerprint, metadata.getMetadata().get(fingerprint).getDate());
        }

        store.setAnnouncedFingerprintsOf(getJid(), fingerprintsAndDates);

        for (OpenPgpV4Fingerprint fingerprint : metadata.getMetadata().keySet()) {
            try {
                PubkeyElement key = PubSubDelegate.fetchPubkey(connection, getJid(), fingerprint);
                if (key == null) {
                    LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                            " can not be imported: Is null");
                    continue;
                }
                PGPPublicKeyRing keyRing = new PGPPublicKeyRing(Base64.decode(key.getDataElement().getB64Data()), new BcKeyFingerprintCalculator());
                store.importPublicKey(getJid(), keyRing);
            } catch (PubSubException.NotAPubSubNodeException | PubSubException.NotALeafNodeException |
                    XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Error fetching public key " + Long.toHexString(fingerprint.getKeyId()), e);
            } catch (PGPException | IOException e) {
                LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                        " can not be imported.", e);
            } catch (MissingUserIdOnKeyException e) {
                LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                        " is missing the user-id \"xmpp:" + getJid() + "\". Refuse to import it.", e);
            }
        }
    }
}
