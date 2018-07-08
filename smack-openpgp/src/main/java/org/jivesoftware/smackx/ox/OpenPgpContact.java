package org.jivesoftware.smackx.ox;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

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
        Set<OpenPgpV4Fingerprint> announced = store.getAnnouncedFingerprintsOf(jid).keySet();

        PGPPublicKeyRingCollection announcedKeysCollection = anyKeys;
        for (PGPPublicKeyRing ring : anyKeys) {

            OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(ring.getPublicKey());

            if (!announced.contains(fingerprint)) {
                announcedKeysCollection = PGPPublicKeyRingCollection.removePublicKeyRing(announcedKeysCollection, ring);
            }
        }
        return announcedKeysCollection;
    }
}
