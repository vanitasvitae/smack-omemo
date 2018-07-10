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
import java.util.Arrays;
import java.util.Set;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ox.callback.backup.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyBackupSelectionCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.NoBackupFoundException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;
import org.jivesoftware.smackx.pubsub.PubSubException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.key.protection.UnprotectedKeysProtector;

public class SecretKeyBackupRestoreIntegrationTest extends AbstractOpenPgpIntegrationTest {

    private static final File beforePath = FileUtils.getTempDir("ox_backup_" + StringUtils.randomString(10));
    private static final File afterPath = FileUtils.getTempDir("ox_restore_" + StringUtils.randomString(10));

    private String backupCode = null;

    public SecretKeyBackupRestoreIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        super(environment);
        if (!OpenPgpManager.serverSupportsSecretKeyBackups(aliceConnection)) {
            throw new TestNotPossibleException("Server does not support the 'whitelist' PubSub access model.");
        }
    }

    @BeforeClass
    @AfterClass
    public static void cleanUp() {
        FileUtils.deleteDirectory(afterPath);
        FileUtils.deleteDirectory(beforePath);
    }

    @After
    @Before
    public void deleteBackup()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubDelegate.deleteSecretKeyNode(aliceConnection);
    }

    @SmackIntegrationTest
    public void test() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException, InterruptedException, PubSubException.NotALeafNodeException,
            SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NotLoggedInException, SmackException.FeatureNotSupportedException,
            MissingUserIdOnKeyException, NoBackupFoundException, InvalidBackupCodeException, PGPException, MissingOpenPgpKeyPairException {

        OpenPgpStore beforeStore = new FileBasedOpenPgpStore(beforePath);
        beforeStore.setKeyRingProtector(new UnprotectedKeysProtector());
        PainlessOpenPgpProvider beforeProvider = new PainlessOpenPgpProvider(beforeStore);
        OpenPgpManager openPgpManager = OpenPgpManager.getInstanceFor(aliceConnection);
        openPgpManager.setOpenPgpProvider(beforeProvider);

        OpenPgpSelf self = openPgpManager.getOpenPgpSelf();

        assertNull(self.getSigningKeyFingerprint());

        OpenPgpV4Fingerprint keyFingerprint = openPgpManager.generateAndImportKeyPair(alice);
        assertEquals(keyFingerprint, self.getSigningKeyFingerprint());

        assertTrue(self.getSecretKeys().contains(keyFingerprint.getKeyId()));

        PGPSecretKeyRing beforeKeys = beforeStore.getSecretKeyRing(alice, keyFingerprint);
        assertNotNull(beforeKeys);

        openPgpManager.backupSecretKeyToServer(new DisplayBackupCodeCallback() {
            @Override
            public void displayBackupCode(String backupCode) {
                SecretKeyBackupRestoreIntegrationTest.this.backupCode = backupCode;
            }
        }, new SecretKeyBackupSelectionCallback() {
            @Override
            public Set<OpenPgpV4Fingerprint> selectKeysToBackup(Set<OpenPgpV4Fingerprint> availableSecretKeys) {
                return availableSecretKeys;
            }
        });

        FileBasedOpenPgpStore afterStore = new FileBasedOpenPgpStore(afterPath);
        afterStore.setKeyRingProtector(new UnprotectedKeysProtector());
        PainlessOpenPgpProvider afterProvider = new PainlessOpenPgpProvider(afterStore);
        openPgpManager.setOpenPgpProvider(afterProvider);

        self = openPgpManager.getOpenPgpSelf();

        assertNull(self.getSigningKeyFingerprint());
        assertFalse(self.getSecretKeys().contains(keyFingerprint.getKeyId()));

        OpenPgpV4Fingerprint fingerprint = openPgpManager.restoreSecretKeyServerBackup(new AskForBackupCodeCallback() {
            @Override
            public String askForBackupCode() {
                return backupCode;
            }
        }, new SecretKeyRestoreSelectionCallback() {
            @Override
            public OpenPgpV4Fingerprint selectSecretKeyToRestore(Set<OpenPgpV4Fingerprint> availableSecretKeys) {
                return availableSecretKeys.iterator().next();
            }
        });

        assertTrue(self.getSecretKeys().contains(keyFingerprint.getKeyId()));

        assertEquals(keyFingerprint, self.getSigningKeyFingerprint());

        PGPSecretKeyRing afterKeys = afterStore.getSecretKeyRing(alice, keyFingerprint);
        assertNotNull(afterKeys);
        assertTrue(Arrays.equals(beforeKeys.getEncoded(), afterKeys.getEncoded()));
    }
}
