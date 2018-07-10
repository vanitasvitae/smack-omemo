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
import java.util.Collection;
import java.util.Collections;
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
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.key.protection.UnprotectedKeysProtector;
import org.pgpainless.pgpainless.util.BCUtil;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenPgpStoreTest extends SmackTestSuite {

    private static File storagePath;

    private static final BareJid alice = JidTestUtil.BARE_JID_1;
    private static final BareJid bob = JidTestUtil.BARE_JID_2;

    private static final OpenPgpV4Fingerprint finger1 = new OpenPgpV4Fingerprint("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    private static final OpenPgpV4Fingerprint finger2 = new OpenPgpV4Fingerprint("ABCDABCDABCDABCDABCDABCDABCDABCDABCDABCD");
    private static final OpenPgpV4Fingerprint finger3 = new OpenPgpV4Fingerprint("0123012301230123012301230123012301230123");

    private final OpenPgpStore store;
    private final OpenPgpStore otherStore;

    static {
        storagePath = FileUtils.getTempDir("storeTest");
        Security.addProvider(new BouncyCastleProvider());
    }

    @Parameterized.Parameters
    public static Collection<OpenPgpStore[]> data() {
        return Arrays.asList(
                new OpenPgpStore[][] {
                        {new FileBasedOpenPgpStore(storagePath), new FileBasedOpenPgpStore(storagePath)}
                });
    }

    public OpenPgpStoreTest(OpenPgpStore store, OpenPgpStore otherStore) {
        this.store = store;
        this.otherStore = otherStore;
    }

    @Before
    @After
    public void deletePath() {
        FileUtils.deleteDirectory(storagePath);
    }

    /*
    Generic
     */

    @Test
    public void t00_store_protectorGetSet() {
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
    public void t00_deleteTest() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, MissingUserIdOnKeyException {
        assertNull(store.getSecretKeysOf(alice));
        assertNull(store.getPublicKeysOf(alice));

        PGPSecretKeyRing keys = store.generateKeyRing(alice);
        store.importSecretKey(alice, keys);

        assertNotNull(store.getSecretKeysOf(alice));
        assertNotNull(store.getPublicKeysOf(alice));

        store.deleteSecretKeyRing(alice, new OpenPgpV4Fingerprint(keys));
        store.deletePublicKeyRing(alice, new OpenPgpV4Fingerprint(keys));

        assertNull(store.getPublicKeysOf(alice));
        assertNull(store.getSecretKeysOf(alice));
    }

    @Test
    public void t01_key_emptyStoreTest() throws IOException, PGPException {
        assertNull(store.getPublicKeysOf(alice));
        assertNull(store.getSecretKeysOf(alice));
        assertNull(store.getPublicKeyRing(alice, finger1));
        assertNull(store.getSecretKeyRing(alice, finger1));
    }

    @Test
    public void t02_key_importPublicKeyFirst() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, MissingUserIdOnKeyException {
        // Test for nullity of all possible values.

        PGPSecretKeyRing secretKeys = store.generateKeyRing(alice);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);
        assertNotNull(secretKeys);
        assertNotNull(publicKeys);

        OpenPgpContact cAlice = store.getOpenPgpContact(alice);
        assertNull(cAlice.getAnyPublicKeys());

        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(publicKeys);
        assertEquals(fingerprint, new OpenPgpV4Fingerprint(secretKeys));

        assertNull(store.getPublicKeysOf(alice));
        assertNull(store.getSecretKeysOf(alice));

        store.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), store.getPublicKeysOf(alice).getEncoded()));
        assertNotNull(store.getPublicKeyRing(alice, fingerprint));
        assertNull(store.getSecretKeysOf(alice));

        cAlice = store.getOpenPgpContact(alice);
        assertNotNull(cAlice.getAnyPublicKeys());

        // Import keys a second time -> No change expected.
        store.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), store.getPublicKeysOf(alice).getEncoded()));
        store.importSecretKey(alice, secretKeys);
        assertTrue(Arrays.equals(secretKeys.getEncoded(), store.getSecretKeysOf(alice).getEncoded()));

        store.importSecretKey(alice, secretKeys);
        assertNotNull(store.getSecretKeysOf(alice));
        assertTrue(Arrays.equals(secretKeys.getEncoded(), store.getSecretKeysOf(alice).getEncoded()));
        assertNotNull(store.getSecretKeyRing(alice, fingerprint));

        assertTrue(Arrays.equals(secretKeys.getEncoded(), store.getSecretKeyRing(alice, fingerprint).getEncoded()));
        assertTrue(Arrays.equals(publicKeys.getEncoded(),
                BCUtil.publicKeyRingFromSecretKeyRing(store.getSecretKeyRing(alice, fingerprint)).getEncoded()));

        // Clean up
        store.deletePublicKeyRing(alice, fingerprint);
        store.deleteSecretKeyRing(alice, fingerprint);
    }

    @Test
    public void t03_key_importSecretKeyFirst() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing secretKeys = store.generateKeyRing(alice);
        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(secretKeys);

        assertNull(store.getSecretKeysOf(alice));
        assertNull(store.getPublicKeysOf(alice));
        store.importSecretKey(alice, secretKeys);

        assertNotNull(store.getSecretKeysOf(alice));
        assertNotNull(store.getPublicKeysOf(alice));

        // Clean up
        store.deleteSecretKeyRing(alice, fingerprint);
        store.deletePublicKeyRing(alice, fingerprint);
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void t04_key_wrongBareJidOnSecretKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing secretKeys = store.generateKeyRing(alice);

        store.importSecretKey(bob, secretKeys);
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void t05_key_wrongBareJidOnPublicKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing secretKeys = store.generateKeyRing(alice);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);

        store.importPublicKey(bob, publicKeys);
    }

    @Test
    public void t06_key_keyReloadTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing secretKeys = store.generateKeyRing(alice);
        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(secretKeys);
        PGPPublicKeyRing publicKeys = BCUtil.publicKeyRingFromSecretKeyRing(secretKeys);

        store.importSecretKey(alice, secretKeys);
        store.importPublicKey(alice, publicKeys);

        assertNotNull(otherStore.getSecretKeysOf(alice));
        assertNotNull(otherStore.getPublicKeysOf(alice));

        // Clean up
        store.deletePublicKeyRing(alice, fingerprint);
        store.deleteSecretKeyRing(alice, fingerprint);
        otherStore.deletePublicKeyRing(alice, fingerprint);
        otherStore.deleteSecretKeyRing(alice, fingerprint);
    }

    @Test
    public void t07_multipleKeysTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing one = store.generateKeyRing(alice);
        PGPSecretKeyRing two = store.generateKeyRing(alice);

        OpenPgpV4Fingerprint fingerprint1 = new OpenPgpV4Fingerprint(one);
        OpenPgpV4Fingerprint fingerprint2 = new OpenPgpV4Fingerprint(two);

        store.importSecretKey(alice, one);
        store.importSecretKey(alice, two);

        assertTrue(Arrays.equals(one.getEncoded(), store.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(one)).getEncoded()));
        assertTrue(Arrays.equals(two.getEncoded(), store.getSecretKeyRing(alice, new OpenPgpV4Fingerprint(two)).getEncoded()));

        assertTrue(Arrays.equals(one.getEncoded(), store.getSecretKeysOf(alice).getSecretKeyRing(one.getPublicKey().getKeyID()).getEncoded()));

        assertTrue(Arrays.equals(BCUtil.publicKeyRingFromSecretKeyRing(one).getEncoded(),
                store.getPublicKeyRing(alice, new OpenPgpV4Fingerprint(one)).getEncoded()));

        // Cleanup
        store.deletePublicKeyRing(alice, fingerprint1);
        store.deletePublicKeyRing(alice, fingerprint2);
        store.deleteSecretKeyRing(alice, fingerprint1);
        store.deleteSecretKeyRing(alice, fingerprint2);
    }

    /*
    OpenPgpTrustStore
     */

    @Test
    public void t08_trust_emptyStoreTest() throws IOException {

        assertEquals(OpenPgpTrustStore.Trust.undecided, store.getTrust(alice, finger2));
        store.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, store.getTrust(alice, finger2));
        // Set trust a second time -> no change
        store.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, store.getTrust(alice, finger2));

        assertEquals(OpenPgpTrustStore.Trust.undecided, store.getTrust(alice, finger3));

        store.setTrust(bob, finger2, OpenPgpTrustStore.Trust.untrusted);
        assertEquals(OpenPgpTrustStore.Trust.untrusted, store.getTrust(bob, finger2));
        assertEquals(OpenPgpTrustStore.Trust.trusted, store.getTrust(alice, finger2));

        // clean up
        store.setTrust(alice, finger2, OpenPgpTrustStore.Trust.undecided);
        store.setTrust(bob, finger2, OpenPgpTrustStore.Trust.undecided);
    }

    @Test
    public void t09_trust_reloadTest() throws IOException {
        store.setTrust(alice, finger1, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, otherStore.getTrust(alice, finger1));

        // cleanup
        store.setTrust(alice, finger1, OpenPgpTrustStore.Trust.undecided);
        otherStore.setTrust(alice, finger1, OpenPgpTrustStore.Trust.undecided);
    }

    /*
    OpenPgpMetadataStore
     */

    @Test
    public void t10_meta_emptyStoreTest() throws IOException {
        assertNotNull(store.getAnnouncedFingerprintsOf(alice));
        assertTrue(store.getAnnouncedFingerprintsOf(alice).isEmpty());

        Map<OpenPgpV4Fingerprint, Date> map = new HashMap<>();
        Date date1 = new Date(12354563423L);
        Date date2 = new Date(8274729879812L);
        map.put(finger1, date1);
        map.put(finger2, date2);

        store.setAnnouncedFingerprintsOf(alice, map);
        assertFalse(store.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, store.getAnnouncedFingerprintsOf(alice));

        assertTrue(store.getAnnouncedFingerprintsOf(bob).isEmpty());

        assertFalse(otherStore.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, otherStore.getAnnouncedFingerprintsOf(alice));

        store.setAnnouncedFingerprintsOf(alice, Collections.<OpenPgpV4Fingerprint, Date>emptyMap());
        otherStore.setAnnouncedFingerprintsOf(alice, Collections.<OpenPgpV4Fingerprint, Date>emptyMap());
    }
}
