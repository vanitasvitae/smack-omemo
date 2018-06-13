package org.jivesoftware.smackx.ox.bouncycastle.selection_strategy;

import java.util.Iterator;

import de.vanitasvitae.crypto.pgpainless.key.selection.keyring.PublicKeyRingSelectionStrategy;
import de.vanitasvitae.crypto.pgpainless.key.selection.keyring.SecretKeyRingSelectionStrategy;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jxmpp.jid.BareJid;

public class BareJidUserId {

    public static class PubRingSelectionStrategy extends PublicKeyRingSelectionStrategy<BareJid> {

        @Override
        public boolean accept(BareJid jid, PGPPublicKeyRing ring) {
            Iterator<String> userIds = ring.getPublicKey().getUserIDs();
            while (userIds.hasNext()) {
                String userId = userIds.next();
                if (userId.equals("xmpp:" + jid.toString())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SecRingSelectionStrategy extends SecretKeyRingSelectionStrategy<BareJid> {

        @Override
        public boolean accept(BareJid jid, PGPSecretKeyRing ring) {
            Iterator<String> userIds = ring.getPublicKey().getUserIDs();
            while (userIds.hasNext()) {
                String userId = userIds.next();
                if (userId.equals("xmpp:" + jid.toString())) {
                    return true;
                }
            }
            return false;
        }
    }
}
