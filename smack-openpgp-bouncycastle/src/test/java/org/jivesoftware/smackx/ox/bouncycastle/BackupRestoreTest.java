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
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.KeyBytesAndFingerprint;
import org.jivesoftware.smackx.ox.util.SecretKeyBackupHelper;

import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;

public class BackupRestoreTest extends OxTestSuite {

    private static final File backupPath = FileUtils.getTempDir("ox-backup");
    private static final File restorePath = FileUtils.getTempDir("ox-restore");
    private static final BareJid owner = JidTestUtil.BARE_JID_1;


    @BeforeClass
    @AfterClass
    public static void deletePaths() {
        FileUtils.deleteDirectory(backupPath);
        FileUtils.deleteDirectory(restorePath);
    }

    @Test
    public void test()
            throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException,
            SmackOpenPgpException, MissingUserIdOnKeyException, InvalidBackupCodeException,
            MissingOpenPgpKeyPairException, MissingOpenPgpPublicKeyException {
        FileBasedPainlessOpenPgpStore backupStore = new FileBasedPainlessOpenPgpStore(backupPath, new UnprotectedKeysProtector());
        FileBasedPainlessOpenPgpStore restoreStore = new FileBasedPainlessOpenPgpStore(restorePath, new UnprotectedKeysProtector());

        PainlessOpenPgpProvider backupProvider = new PainlessOpenPgpProvider(owner, backupStore);
        PainlessOpenPgpProvider restoreProvider = new PainlessOpenPgpProvider(owner, restoreStore);

        KeyBytesAndFingerprint key = backupProvider.generateOpenPgpKeyPair(owner);
        backupProvider.importSecretKey(key.getBytes());

        final String backupCode = SecretKeyBackupHelper.generateBackupPassword();
        SecretkeyElement backup = SecretKeyBackupHelper.createSecretkeyElement(backupProvider, owner, Collections.singleton(key.getFingerprint()), backupCode);

        OpenPgpV4Fingerprint fingerprint = SecretKeyBackupHelper.restoreSecretKeyBackup(restoreProvider, backup, backupCode);

        assertEquals(key.getFingerprint(), fingerprint);

        assertTrue(Arrays.equals(backupStore.getSecretKeyRingBytes(owner, fingerprint), restoreStore.getSecretKeyRingBytes(owner, fingerprint)));
        assertTrue(Arrays.equals(backupStore.getPublicKeyRingBytes(owner, fingerprint), restoreStore.getPublicKeyRingBytes(owner, fingerprint)));
    }
}
