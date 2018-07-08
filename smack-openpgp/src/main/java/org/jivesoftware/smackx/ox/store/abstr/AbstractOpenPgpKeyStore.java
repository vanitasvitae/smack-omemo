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
package org.jivesoftware.smackx.ox.store.abstr;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.selection_strategy.BareJidUserId;
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

    private static final Logger LOGGER = Logger.getLogger(AbstractOpenPgpKeyStore.class.getName());

    protected Map<BareJid, PGPPublicKeyRingCollection> publicKeyRingCollections = new HashMap<>();
    protected Map<BareJid, PGPSecretKeyRingCollection> secretKeyRingCollections = new HashMap<>();

    protected abstract PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner) throws IOException, PGPException;

    protected abstract void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException;

    protected abstract PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException;

    protected abstract void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException;

    @Override
    public PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException {
        PGPPublicKeyRingCollection keys = publicKeyRingCollections.get(owner);
        if (keys == null) {
            keys = readPublicKeysOf(owner);
            if (keys != null) {
                publicKeyRingCollections.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException {
        PGPSecretKeyRingCollection keys = secretKeyRingCollections.get(owner);
        if (keys == null) {
            keys = readSecretKeysOf(owner);
            if (keys != null) {
                secretKeyRingCollections.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public void importSecretKey(BareJid owner, PGPSecretKeyRing secretKeys)
            throws IOException, PGPException, MissingUserIdOnKeyException {

        if (!new BareJidUserId.SecRingSelectionStrategy().accept(owner, secretKeys)) {
            throw new MissingUserIdOnKeyException(owner, secretKeys.getPublicKey().getKeyID());
        }

        PGPSecretKeyRingCollection secretKeyRings = getSecretKeysOf(owner);
        try {
            secretKeyRings = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRings, secretKeys);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Skipping secret key ring " + Long.toHexString(secretKeys.getPublicKey().getKeyID()) +
                    " as it is already in the key ring of " + owner.toString());
        }
        this.secretKeyRingCollections.put(owner, secretKeyRings);
        writeSecretKeysOf(owner, secretKeyRings);
    }

    @Override
    public void importPublicKey(BareJid owner, PGPPublicKeyRing publicKeys) throws IOException, PGPException, MissingUserIdOnKeyException {

        if (!new BareJidUserId.PubRingSelectionStrategy().accept(owner, publicKeys)) {
            throw new MissingUserIdOnKeyException(owner, publicKeys.getPublicKey().getKeyID());
        }

        PGPPublicKeyRingCollection publicKeyRings = getPublicKeysOf(owner);
        try {
            publicKeyRings = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRings, publicKeys);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Skipping public key ring " + Long.toHexString(publicKeys.getPublicKey().getKeyID()) +
                    " as it is already in the key ring of " + owner.toString());
        }
        this.publicKeyRingCollections.put(owner, publicKeyRings);
        writePublicKeysOf(owner, publicKeyRings);
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
