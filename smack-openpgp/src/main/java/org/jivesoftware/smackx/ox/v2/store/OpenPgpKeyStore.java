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

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;

public interface OpenPgpKeyStore {

    PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner) throws IOException, PGPException;

    void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException;

    PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException;

    PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException;

    void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException;

    PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException;

    PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    PGPSecretKeyRing generateKeyRing(BareJid owner) throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException;
}
