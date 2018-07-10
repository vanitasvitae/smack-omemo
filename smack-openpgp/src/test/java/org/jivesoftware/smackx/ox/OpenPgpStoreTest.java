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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.key.protection.UnprotectedKeysProtector;
import org.pgpainless.pgpainless.util.BCUtil;

public class OpenPgpStoreTest extends SmackTestSuite {

    private static File storagePath;

    private static final BareJid alice = JidTestUtil.BARE_JID_1;
    private static final BareJid bob = JidTestUtil.BARE_JID_2;

    private static final OpenPgpV4Fingerprint finger1 = new OpenPgpV4Fingerprint("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    private static final OpenPgpV4Fingerprint finger2 = new OpenPgpV4Fingerprint("ABCDABCDABCDABCDABCDABCDABCDABCDABCDABCD");
    private static final OpenPgpV4Fingerprint finger3 = new OpenPgpV4Fingerprint("0123012301230123012301230123012301230123");

    static {
        storagePath = FileUtils.getTempDir("storeTest");
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeClass
    @AfterClass
    public static void deletePath() {
        FileUtils.deleteDirectory(storagePath);
    }

    /*
    Generic
     */

    @Test
    public void store_protectorGetSet() {
        OpenPgpStore store = new FileBasedOpenPgpStore(new File(storagePath, "store_protector"));
        assertNull(store.getKeyRingProtector());
        store.setKeyRingProtector(new UnprotectedKeysProtector());
        assertNotNull(store.getKeyRingProtector());
        // TODO: Test method below
        store.setSecretKeyPassphraseCallback(new SecretKeyPassphraseCallback() {
            @Override
            public char[] onPassphraseNeeded(OpenPgpV4Fingerprint fingerprint) {
                return new char[0];
            }
        });
    }

    /*
    OpenPgpKeyStore
     */

    @Test
    public void key_emptyStoreTest() throws IOException, PGPException {
        FileBasedOpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_empty"));
        assertNull(keyStore.getPublicKeysOf(alice));
        assertNull(keyStore.getSecretKeysOf(alice));
        assertNull(keyStore.getPublicKeyRing(alice, finger1));
        assertNull(keyStore.getSecretKeyRing(alice, finger1));
    }

    @Test
    public void key_importPublicKeyFirst() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, MissingUserIdOnKeyException {
        // Test for nullity of all possible values.
        FileBasedOpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_publicFirst"));

        PGPSecretKeyRing secretKeys = keyStore.generateKeyRing(alice);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);
        assertNotNull(secretKeys);
        assertNotNull(publicKeys);

        OpenPgpContact cAlice = keyStore.getOpenPgpContact(alice);
        assertNull(cAlice.getAnyPublicKeys());

        assertEquals(new OpenPgpV4Fingerprint(publicKeys), new OpenPgpV4Fingerprint(secretKeys));

        assertNull(keyStore.getPublicKeysOf(alice));
        assertNull(keyStore.getSecretKeysOf(alice));

        keyStore.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), keyStore.getPublicKeysOf(alice).getEncoded()));
        assertNotNull(keyStore.getPublicKeyRing(alice, new OpenPgpV4Fingerprint(publicKeys)));
        assertNull(keyStore.getSecretKeysOf(alice));

        cAlice = keyStore.getOpenPgpContact(alice);
        assertNotNull(cAlice.getAnyPublicKeys());

        // Import keys a second time -> No change expected.
        keyStore.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), keyStore.getPublicKeysOf(alice).getEncoded()));
        keyStore.importSecretKey(alice, secretKeys);
        assertTrue(Arrays.equals(secretKeys.getEncoded(), keyStore.getSecretKeysOf(alice).getEncoded()));

        keyStore.importSecretKey(alice, secretKeys);
        assertNotNull(keyStore.getSecretKeysOf(alice));
        assertTrue(Arrays.equals(secretKeys.getEncoded(), keyStore.getSecretKeysOf(alice).getEncoded()));
        assertNotNull(keyStore.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(secretKeys)));

        assertTrue(Arrays.equals(secretKeys.getEncoded(), keyStore.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(secretKeys)).getEncoded()));
        assertTrue(Arrays.equals(publicKeys.getEncoded(),
                BCUtil.publicKeyRingFromSecretKeyRing(keyStore.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(secretKeys))).getEncoded()));
    }

    @Test
    public void key_importSecretKeyFirst() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        FileBasedOpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_secretFirst"));
        PGPSecretKeyRing secretKeys = keyStore.generateKeyRing(alice);

        assertNull(keyStore.getSecretKeysOf(alice));
        assertNull(keyStore.getPublicKeysOf(alice));
        keyStore.importSecretKey(alice, secretKeys);

        assertNotNull(keyStore.getSecretKeysOf(alice));
        assertNotNull(keyStore.getPublicKeysOf(alice));
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void key_wrongBareJidOnSecretKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        FileBasedOpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_wrongSecBareJid"));
        PGPSecretKeyRing secretKeys = keyStore.generateKeyRing(alice);

        keyStore.importSecretKey(bob, secretKeys);
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void key_wrongBareJidOnPublicKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        FileBasedOpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_wrongPubBareJid"));
        PGPSecretKeyRing secretKeys = keyStore.generateKeyRing(alice);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);

        keyStore.importPublicKey(bob, publicKeys);
    }

    @Test
    public void key_keyReloadTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        FileBasedOpenPgpStore one = new FileBasedOpenPgpStore(new File(storagePath, "keys_reload"));
        PGPSecretKeyRing secretKeys = one.generateKeyRing(alice);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);

        one.importSecretKey(alice, secretKeys);
        one.importPublicKey(alice, publicKeys);

        FileBasedOpenPgpStore two = new FileBasedOpenPgpStore(new File(storagePath, "keys_reload"));
        assertNotNull(two.getSecretKeysOf(alice));
        assertNotNull(two.getPublicKeysOf(alice));
    }

    @Test
    public void multipleKeysTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        OpenPgpStore keyStore = new FileBasedOpenPgpStore(new File(storagePath, "keys_multi"));
        PGPSecretKeyRing one = keyStore.generateKeyRing(alice);
        PGPSecretKeyRing two = keyStore.generateKeyRing(alice);

        keyStore.importSecretKey(alice, one);
        keyStore.importSecretKey(alice, two);

        assertTrue(Arrays.equals(one.getEncoded(), keyStore.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(one)).getEncoded()));
        assertTrue(Arrays.equals(two.getEncoded(), keyStore.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(two)).getEncoded()));

        assertTrue(Arrays.equals(one.getEncoded(), keyStore.getSecretKeysOf(alice).getSecretKeyRing(one.getPublicKey().getKeyID()).getEncoded()));

        assertTrue(Arrays.equals(BCUtil.publicKeyRingFromSecretKeyRing(one).getEncoded(),
                keyStore.getPublicKeyRing(alice, new OpenPgpV4Fingerprint(one)).getEncoded()));
    }

    /*
    OpenPgpTrustStore
     */

    @Test
    public void trust_emptyStoreTest() throws IOException {

        FileBasedOpenPgpStore trustStore = new FileBasedOpenPgpStore(new File(storagePath, "trust_empty"));
        assertEquals(OpenPgpTrustStore.Trust.undecided, trustStore.getTrust(alice, finger2));
        trustStore.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, trustStore.getTrust(alice, finger2));
        // Set trust a second time -> no change
        trustStore.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, trustStore.getTrust(alice, finger2));

        assertEquals(OpenPgpTrustStore.Trust.undecided, trustStore.getTrust(alice, finger3));

        trustStore.setTrust(bob, finger2, OpenPgpTrustStore.Trust.untrusted);
        assertEquals(OpenPgpTrustStore.Trust.untrusted, trustStore.getTrust(bob, finger2));
        assertEquals(OpenPgpTrustStore.Trust.trusted, trustStore.getTrust(alice, finger2));
    }

    @Test
    public void trust_reloadTest() throws IOException {
        OpenPgpStore trustStore = new FileBasedOpenPgpStore(new File(storagePath, "trust_reload"));
        trustStore.setTrust(alice, finger1, OpenPgpTrustStore.Trust.trusted);

        OpenPgpStore secondStore = new FileBasedOpenPgpStore(new File(storagePath, "trust_reload"));
        assertEquals(OpenPgpTrustStore.Trust.trusted, secondStore.getTrust(alice, finger1));
    }

    /*
    OpenPgpMetadataStore
     */

    @Test
    public void meta_emptyStoreTest() throws IOException {
        OpenPgpStore metaStore = new FileBasedOpenPgpStore(new File(storagePath, "meta_empty"));
        assertNotNull(metaStore.getAnnouncedFingerprintsOf(alice));
        assertTrue(metaStore.getAnnouncedFingerprintsOf(alice).isEmpty());

        Map<OpenPgpV4Fingerprint, Date> map = new HashMap<>();
        Date date1 = new Date(12354563423L);
        Date date2 = new Date(8274729879812L);
        map.put(finger1, date1);
        map.put(finger2, date2);

        metaStore.setAnnouncedFingerprintsOf(alice, map);
        assertFalse(metaStore.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, metaStore.getAnnouncedFingerprintsOf(alice));

        assertTrue(metaStore.getAnnouncedFingerprintsOf(bob).isEmpty());

        OpenPgpStore otherStore = new FileBasedOpenPgpStore(new File(storagePath, "meta_empty"));
        assertFalse(otherStore.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, otherStore.getAnnouncedFingerprintsOf(alice));
    }
}
