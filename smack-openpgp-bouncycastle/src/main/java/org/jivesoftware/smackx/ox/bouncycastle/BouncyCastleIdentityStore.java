package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;

public interface BouncyCastleIdentityStore {

    void storePubkeyList(BareJid jid, PublicKeysListElement list) throws FileNotFoundException, IOException;

    PublicKeysListElement loadPubkeyList(BareJid jid) throws FileNotFoundException, IOException;

    void storePublicKeys(BareJid jid, PGPPublicKeyRingCollection keys);

    PGPPublicKeyRingCollection loadPublicKeys(BareJid jid);

    void storeSecretKeys(PGPSecretKeyRingCollection secretKeys);

    PGPSecretKeyRingCollection loadSecretKeys();

}
