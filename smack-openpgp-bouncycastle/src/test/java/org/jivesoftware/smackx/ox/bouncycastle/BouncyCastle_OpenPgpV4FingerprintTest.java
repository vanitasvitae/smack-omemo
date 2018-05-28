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

import static junit.framework.TestCase.assertEquals;
import static org.jivesoftware.smack.util.StringUtils.UTF8;

import java.io.IOException;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.TestKeys;
import org.jivesoftware.smackx.ox.Util;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;

public class BouncyCastle_OpenPgpV4FingerprintTest extends SmackTestSuite {

    @Test
    public void keyIdFromFingerprintTest() throws IOException, PGPException {
        // Parse the key
        InMemoryKeyring keyringJuliet = KeyringConfigs.forGpgExportedKeys(
                KeyringConfigCallbacks.withUnprotectedKeys());
        keyringJuliet.addPublicKey(TestKeys.JULIET_PUB.getBytes(UTF8));
        PGPPublicKey publicKey = keyringJuliet.getPublicKeyRings().iterator().next().getPublicKey();

        OpenPgpV4Fingerprint fp = BCOpenPgpProvider.getFingerprint(publicKey);
        assertEquals(publicKey.getKeyID(), Util.keyIdFromFingerprint(fp));
    }
}
