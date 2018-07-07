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
package org.jivesoftware.smackx.ox.v2.store;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.protection.SecretKeyRingProtector;

public abstract class OpenPgpStore implements OpenPgpKeyStore, OpenPgpMetadataStore, OpenPgpTrustStore {

    protected final OpenPgpKeyStore keyStore;
    protected final OpenPgpMetadataStore metadataStore;
    protected final OpenPgpTrustStore trustStore;

    protected SecretKeyPassphraseCallback secretKeyPassphraseCallback;
    protected final Map<BareJid, OpenPgpContact> contacts = new HashMap<>();

    public OpenPgpStore(OpenPgpKeyStore keyStore,
                        OpenPgpMetadataStore metadataStore,
                        OpenPgpTrustStore trustStore) {
        this.keyStore = keyStore;
        this.metadataStore = metadataStore;
        this.trustStore = trustStore;
    }

    public OpenPgpContact getContact(BareJid jid) {
        OpenPgpContact contact = contacts.get(jid);
        if (contact != null) {
            return contact;
        }

        // TODO
        return null;
    }

    public void setKeyRingProtector(SecretKeyRingProtector protector) {

    }

    public void setUnknownSecretKeyPassphraseCallback(SecretKeyPassphraseCallback callback) {
        this.secretKeyPassphraseCallback = callback;
    }

    /*
    OpenPgpKeyStore
     */

    @Override
    public PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.readPublicKeysOf(owner);
    }

    @Override
    public void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException {
        keyStore.writePublicKeysOf(owner, publicKeys);
    }

    @Override
    public PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.getPublicKeysOf(owner);
    }

    @Override
    public PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.readSecretKeysOf(owner);
    }

    @Override
    public void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException {
        keyStore.writeSecretKeysOf(owner, secretKeys);
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.getSecretKeysOf(owner);
    }

    @Override
    public PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        return keyStore.getPublicKeyRing(owner, fingerprint);
    }

    @Override
    public PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        return keyStore.getSecretKeyRing(owner, fingerprint);
    }

    @Override
    public PGPSecretKeyRing generateKeyRing(BareJid owner) throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        return keyStore.generateKeyRing(owner);
    }

    /*
    OpenPgpMetadataStore
     */

    @Override
    public Set<OpenPgpV4Fingerprint> getAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        return metadataStore.getAnnouncedFingerprintsOf(contact);
    }

    @Override
    public Set<OpenPgpV4Fingerprint> readAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        return metadataStore.readAnnouncedFingerprintsOf(contact);
    }

    @Override
    public void writeAnnouncedFingerprintsOf(BareJid contact, PublicKeysListElement metadata) throws IOException {
        metadataStore.writeAnnouncedFingerprintsOf(contact, metadata);
    }

    /*
    OpenPgpTrustStore
     */

    @Override
    public Trust getTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        return trustStore.getTrust(owner, fingerprint);
    }

    @Override
    public void setTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) {
        trustStore.setTrust(owner, fingerprint, trust);
    }
}
