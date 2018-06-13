package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.jxmpp.jid.BareJid;

public abstract class AbstractPainlessOpenPgpStore implements PainlessOpenPgpStore {

    private final BcKeyFingerprintCalculator fingerprintCalculator = new BcKeyFingerprintCalculator();

    private final Map<BareJid, PGPPublicKeyRingCollection> publicKeyRings = new HashMap<>();
    private final Map<BareJid, PGPSecretKeyRingCollection> secretKeyRings = new HashMap<>();

    @Override
    public PGPPublicKeyRingCollection getPublicKeyRings(BareJid owner) throws IOException, PGPException {
        PGPPublicKeyRingCollection keyRing = publicKeyRings.get(owner);
        if (keyRing != null) {
            return keyRing;
        }

        byte[] bytes = loadPublicKeyRingBytes(owner);
        keyRing = new PGPPublicKeyRingCollection(bytes, fingerprintCalculator);

        publicKeyRings.put(owner, keyRing);

        return keyRing;
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeyRing(BareJid owner) throws IOException, PGPException {
        PGPSecretKeyRingCollection keyRing = secretKeyRings.get(owner);
        if (keyRing != null) {
            return keyRing;
        }

        byte[] bytes = loadSecretKeyRingBytes(owner);
        keyRing = new PGPSecretKeyRingCollection(bytes, fingerprintCalculator);

        secretKeyRings.put(owner, keyRing);

        return keyRing;
    }

    @Override
    public void storePublicKeyRing(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException {
        publicKeyRings.put(owner, publicKeys);
        storePublicKeyRingBytes(owner, publicKeys.getEncoded());
    }

    @Override
    public void storeSecretKeyRing(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException {
        secretKeyRings.put(owner, secretKeys);
        storeSecretKeyRingBytes(owner, secretKeys.getEncoded());
    }

}
