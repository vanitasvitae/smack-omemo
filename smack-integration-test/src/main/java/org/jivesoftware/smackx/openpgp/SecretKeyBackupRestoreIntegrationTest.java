package org.jivesoftware.smackx.openpgp;

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
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.bouncycastle.FileBasedPainlessOpenPgpStore;
import org.jivesoftware.smackx.ox.bouncycastle.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.callback.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.SecretKeyBackupSelectionCallback;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.NoBackupFoundException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;
import org.jivesoftware.smackx.pubsub.PubSubException;

import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import org.bouncycastle.openpgp.PGPException;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class SecretKeyBackupRestoreIntegrationTest extends AbstractOpenPgpIntegrationTest {

    private static final File beforePath = FileUtils.getTempDir("ox_backup_" + StringUtils.randomString(10));
    private static final File afterPath = FileUtils.getTempDir("ox_restore_" + StringUtils.randomString(10));

    private String backupCode = null;

    public SecretKeyBackupRestoreIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException, SmackException.NotLoggedInException {
        super(environment);
        if (!OpenPgpManager.serverSupportsSecretKeyBackups(aliceConnection)) {
            throw new TestNotPossibleException("Server does not support the whitelist access model.");
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
    public void test() throws SmackOpenPgpException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException, InterruptedException, PubSubException.NotALeafNodeException,
            SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NotLoggedInException, SmackException.FeatureNotSupportedException,
            MissingUserIdOnKeyException, NoBackupFoundException, InvalidBackupCodeException, PGPException {

        FileBasedPainlessOpenPgpStore beforeStore = new FileBasedPainlessOpenPgpStore(beforePath, new UnprotectedKeysProtector());
        PainlessOpenPgpProvider beforeProvider = new PainlessOpenPgpProvider(alice, beforeStore);
        OpenPgpManager openPgpManager = OpenPgpManager.getInstanceFor(aliceConnection);
        openPgpManager.setOpenPgpProvider(beforeProvider);

        beforeStore.setPrimaryOpenPgpKeyPairFingerprint(
                openPgpManager.generateAndImportKeyPair(alice));

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

        FileBasedPainlessOpenPgpStore afterStore = new FileBasedPainlessOpenPgpStore(afterPath, new UnprotectedKeysProtector());
        PainlessOpenPgpProvider afterProvider = new PainlessOpenPgpProvider(alice, afterStore);
        openPgpManager.setOpenPgpProvider(afterProvider);

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

        afterStore.setPrimaryOpenPgpKeyPairFingerprint(fingerprint);

        assertTrue(Arrays.equals(beforeStore.getSecretKeyRings(alice).getEncoded(), afterStore.getSecretKeyRings(alice).getEncoded()));
    }
}
