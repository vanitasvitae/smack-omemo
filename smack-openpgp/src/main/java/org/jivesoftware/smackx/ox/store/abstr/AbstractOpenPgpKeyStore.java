/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.store.abstr;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.ox.store.definition.OpenPgpKeyStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.PGPainless;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.key.generation.type.length.RsaLength;

public abstract class AbstractOpenPgpKeyStore implements OpenPgpKeyStore {

    public AbstractOpenPgpKeyStore() {

    }

    protected Map<BareJid, PGPPublicKeyRingCollection> publicKeys = new HashMap<>();
    protected Map<BareJid, PGPSecretKeyRingCollection> secretKeys = new HashMap<>();

    protected abstract PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner) throws IOException, PGPException;

    protected abstract void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException;

    protected abstract PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException;

    protected abstract void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException;

    @Override
    public PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException {
        PGPPublicKeyRingCollection keys = publicKeys.get(owner);
        if (keys == null) {
            keys = readPublicKeysOf(owner);
            if (keys != null) {
                publicKeys.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException {
        PGPSecretKeyRingCollection keys = secretKeys.get(owner);
        if (keys == null) {
            keys = readSecretKeysOf(owner);
            if (keys != null) {
                secretKeys.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPPublicKeyRingCollection publicKeyRings = getPublicKeysOf(owner);

        if (publicKeyRings != null) {
            return publicKeyRings.getPublicKeyRing(fingerprint.getKeyId());
        }

        return null;
    }

    @Override
    public PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPSecretKeyRingCollection secretKeyRings = getSecretKeysOf(owner);

        if (secretKeyRings != null) {
            return secretKeyRings.getSecretKeyRing(fingerprint.getKeyId());
        }

        return null;
    }

    @Override
    public PGPSecretKeyRing generateKeyRing(BareJid owner)
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        return PGPainless.generateKeyRing().simpleRsaKeyRing("xmpp:" + owner.toString(), RsaLength._4096);
    }
}
