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
