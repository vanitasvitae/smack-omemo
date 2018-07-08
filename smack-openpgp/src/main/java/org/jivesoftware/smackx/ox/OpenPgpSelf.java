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

import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;

public class OpenPgpSelf extends OpenPgpContact {

    public OpenPgpSelf(BareJid jid, OpenPgpStore store) {
        super(jid, store);
    }

    public boolean hasSecretKeyAvailable() throws IOException, PGPException {
        return getSecretKeys() != null;
    }

    public PGPSecretKeyRingCollection getSecretKeys() throws IOException, PGPException {
        return store.getSecretKeysOf(jid);
    }

    public PGPSecretKeyRing getSigningKeyRing() throws IOException, PGPException {
        PGPSecretKeyRingCollection secretKeyRings = getSecretKeys();
        if (secretKeyRings == null) {
            return null;
        }

        PGPSecretKeyRing signingKeyRing = null;
        for (PGPSecretKeyRing ring : secretKeyRings) {
            if (signingKeyRing == null) {
                signingKeyRing = ring;
                continue;
            }

            if (ring.getPublicKey().getCreationTime().after(signingKeyRing.getPublicKey().getCreationTime())) {
                signingKeyRing = ring;
            }
        }

        return signingKeyRing;
    }

    public OpenPgpV4Fingerprint getSigningKeyFingerprint() throws IOException, PGPException {
        PGPSecretKeyRing signingKeyRing = getSigningKeyRing();
        return signingKeyRing != null ? new OpenPgpV4Fingerprint(signingKeyRing.getPublicKey()) : null;
    }
}
