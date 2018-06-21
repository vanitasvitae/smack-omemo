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
package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import de.vanitasvitae.crypto.pgpainless.key.SecretKeyRingProtector;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.jxmpp.jid.BareJid;

public abstract class AbstractPainlessOpenPgpStore implements PainlessOpenPgpStore {

    private final BcKeyFingerprintCalculator fingerprintCalculator = new BcKeyFingerprintCalculator();

    private final Map<BareJid, PGPPublicKeyRingCollection> publicKeyRings = new HashMap<>();
    private final Map<BareJid, PGPSecretKeyRingCollection> secretKeyRings = new HashMap<>();
    private OpenPgpV4Fingerprint primaryKeyFingerprint = null;
    private final SecretKeyRingProtector secretKeyRingProtector;

    @Override
    public OpenPgpV4Fingerprint getPrimaryOpenPgpKeyPairFingerprint() {
        return primaryKeyFingerprint;
    }

    @Override
    public void setPrimaryOpenPgpKeyPairFingerprint(OpenPgpV4Fingerprint fingerprint) {
        this.primaryKeyFingerprint = fingerprint;
    }

    @Override
    public SecretKeyRingProtector getSecretKeyProtector() {
        return secretKeyRingProtector;
    }

    public AbstractPainlessOpenPgpStore(SecretKeyRingProtector secretKeyRingProtector) {
        this.secretKeyRingProtector = secretKeyRingProtector;
    }

    @Override
    public PGPPublicKeyRingCollection getPublicKeyRings(BareJid owner) throws IOException, PGPException {
        PGPPublicKeyRingCollection keyRing = publicKeyRings.get(owner);
        if (keyRing != null) {
            return keyRing;
        }

        byte[] bytes = loadPublicKeyRingBytes(owner);
        if (bytes == null) {
            return null;
        }
        keyRing = new PGPPublicKeyRingCollection(bytes, fingerprintCalculator);

        publicKeyRings.put(owner, keyRing);

        return keyRing;
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeyRings(BareJid owner) throws IOException, PGPException {
        PGPSecretKeyRingCollection keyRing = secretKeyRings.get(owner);
        if (keyRing != null) {
            return keyRing;
        }

        byte[] bytes = loadSecretKeyRingBytes(owner);
        if (bytes == null) {
            return null;
        }
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
