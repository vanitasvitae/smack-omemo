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

import java.io.FileNotFoundException;
import java.io.IOException;

import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;

public interface BouncyCastleIdentityStore {

    void storeActivePubkeyList(BareJid jid, PublicKeysListElement list) throws FileNotFoundException, IOException;

    PublicKeysListElement loadActivePubkeyList(BareJid jid) throws FileNotFoundException, IOException;

    void storePublicKeys(BareJid jid, PGPPublicKeyRingCollection keys);

    PGPPublicKeyRingCollection loadPublicKeys(BareJid jid);

    void storeSecretKeys(PGPSecretKeyRingCollection secretKeys);

    PGPSecretKeyRingCollection loadSecretKeys();

}
