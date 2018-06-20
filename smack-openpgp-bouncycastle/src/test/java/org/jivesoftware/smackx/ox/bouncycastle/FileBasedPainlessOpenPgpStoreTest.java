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
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Collections;

import org.jivesoftware.smack.util.FileUtils;

import de.vanitasvitae.crypto.pgpainless.PGPainless;
import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import de.vanitasvitae.crypto.pgpainless.key.generation.type.length.RsaLength;
import de.vanitasvitae.crypto.pgpainless.util.BCUtil;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;

public class FileBasedPainlessOpenPgpStoreTest extends OxTestSuite {

    private final File basePath;
    private final BareJid alice;
    private final BareJid bob;

    private FileBasedPainlessOpenPgpStore store;

    public FileBasedPainlessOpenPgpStoreTest() {
        super();
        this.basePath = FileUtils.getTempDir("ox-filebased-store-test");
        this.alice = JidTestUtil.BARE_JID_1;
        this.bob = JidTestUtil.BARE_JID_2;
    }

    @After
    @Before
    public void deleteStore() {
        FileUtils.deleteDirectory(basePath);
        this.store = new FileBasedPainlessOpenPgpStore(basePath, new UnprotectedKeysProtector());
    }

    @Test
    public void storeSecretKeyRingsTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException {
        PGPSecretKeyRing secretKey = PGPainless.generateKeyRing().simpleRsaKeyRing("xmpp:" + alice.toString(), RsaLength._3072);
        PGPSecretKeyRingCollection saving = new PGPSecretKeyRingCollection(Collections.singleton(secretKey));
        store.storeSecretKeyRing(alice, saving);

        FileBasedPainlessOpenPgpStore store2 = new FileBasedPainlessOpenPgpStore(basePath, new UnprotectedKeysProtector());
        PGPSecretKeyRingCollection restored = store2.getSecretKeyRings(alice);

        assertTrue(Arrays.equals(saving.getEncoded(), restored.getEncoded()));
    }

    @Test
    public void storePublicKeyRingTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException {
        PGPSecretKeyRing secretKeys = PGPainless.generateKeyRing().simpleRsaKeyRing("xmpp:" + alice.toString(), RsaLength._3072);

        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);
        for (PGPSecretKey k : secretKeys) {
            assertEquals(publicKeys.getPublicKey(k.getKeyID()), k.getPublicKey());
        }

        PGPPublicKeyRingCollection saving = new PGPPublicKeyRingCollection(Collections.singleton(publicKeys));
        store.storePublicKeyRing(alice, saving);

        FileBasedPainlessOpenPgpStore store2 = new FileBasedPainlessOpenPgpStore(basePath, new UnprotectedKeysProtector());

        PGPPublicKeyRingCollection restored = store2.getPublicKeyRings(alice);
        assertTrue(Arrays.equals(saving.getEncoded(), restored.getEncoded()));
    }
}
