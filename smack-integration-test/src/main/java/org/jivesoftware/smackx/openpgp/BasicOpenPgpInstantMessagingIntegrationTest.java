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
package org.jivesoftware.smackx.openpgp;

import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ox.OXInstantMessagingManager;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.bouncycastle.FileBasedPainlessOpenPgpStore;
import org.jivesoftware.smackx.ox.bouncycastle.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.chat.OpenPgpContact;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.listener.OxMessageListener;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.pgpainless.pgpainless.key.UnprotectedKeysProtector;

public class BasicOpenPgpInstantMessagingIntegrationTest extends AbstractOpenPgpIntegrationTest {

    private static final File aliceStorePath = FileUtils.getTempDir("basic_ox_messaging_test_alice_" + StringUtils.randomString(10));
    private static final File bobStorePath = FileUtils.getTempDir("basic_ox_messaging_test_bob_" + StringUtils.randomString(10));

    private OpenPgpV4Fingerprint aliceFingerprint = null;
    private OpenPgpV4Fingerprint bobFingerprint = null;

    public BasicOpenPgpInstantMessagingIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, InterruptedException, SmackException.NotConnectedException,
            TestNotPossibleException, SmackException.NoResponseException {
        super(environment);
    }

    @BeforeClass
    @AfterClass
    public static void deleteStore() {
        FileUtils.deleteDirectory(aliceStorePath);
        FileUtils.deleteDirectory(bobStorePath);
    }

    @SmackIntegrationTest
    public void basicInstantMessagingTest()
            throws Exception {

        LOGGER.log(Level.INFO, aliceStorePath.getAbsolutePath() + " " + bobStorePath.getAbsolutePath());

        final SimpleResultSyncPoint bobReceivedMessage = new SimpleResultSyncPoint();
        final String body = "Writing integration tests is an annoying task, but it has to be done, so lets do it!!!";

        FileBasedPainlessOpenPgpStore aliceStore = new FileBasedPainlessOpenPgpStore(aliceStorePath, new UnprotectedKeysProtector());
        FileBasedPainlessOpenPgpStore bobStore = new FileBasedPainlessOpenPgpStore(bobStorePath, new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(alice, aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(bob, bobStore);

        OpenPgpManager aliceOpenPgp = OpenPgpManager.getInstanceFor(aliceConnection);
        OpenPgpManager bobOpenPgp = OpenPgpManager.getInstanceFor(bobConnection);

        OXInstantMessagingManager aliceInstantMessaging = OXInstantMessagingManager.getInstanceFor(aliceConnection);
        OXInstantMessagingManager bobInstantMessaging = OXInstantMessagingManager.getInstanceFor(bobConnection);

        bobInstantMessaging.addOxMessageListener(new OxMessageListener() {
            @Override
            public void newIncomingOxMessage(OpenPgpContact contact, Message originalMessage, SigncryptElement decryptedPayload) {
                if (((Message.Body) decryptedPayload.getExtension(Message.Body.NAMESPACE)).getMessage().equals(body)) {
                    bobReceivedMessage.signal();
                } else {
                    bobReceivedMessage.signalFailure();
                }
            }
        });

        aliceOpenPgp.setOpenPgpProvider(aliceProvider);
        bobOpenPgp.setOpenPgpProvider(bobProvider);

        aliceFingerprint = aliceOpenPgp.generateAndImportKeyPair(alice);
        bobFingerprint = bobOpenPgp.generateAndImportKeyPair(bob);

        aliceStore.setSigningKeyPairFingerprint(aliceFingerprint);
        bobStore.setSigningKeyPairFingerprint(bobFingerprint);

        aliceOpenPgp.announceSupportAndPublish();
        bobOpenPgp.announceSupportAndPublish();

        LOGGER.log(Level.INFO, "Request metadata of bob");
        aliceOpenPgp.requestMetadataUpdate(bob);
        LOGGER.log(Level.INFO, "Request metadata of alice");
        bobOpenPgp.requestMetadataUpdate(alice);

        OpenPgpContact bobForAlice = aliceOpenPgp.getOpenPgpContact(bob.asEntityBareJidIfPossible());
        OpenPgpContact aliceForBob = bobOpenPgp.getOpenPgpContact(alice.asEntityBareJidIfPossible());

        assertTrue(bobForAlice.getFingerprints().getActiveKeys().contains(bobFingerprint));
        assertTrue(aliceForBob.getFingerprints().getActiveKeys().contains(aliceFingerprint));

        aliceInstantMessaging.sendOxMessage(bobForAlice, body);

        bobReceivedMessage.waitForResult(timeout);
    }

    @After
    public void deleteKeyMetadata()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubDelegate.deletePubkeysListNode(aliceConnection);
        PubSubDelegate.deletePubkeysListNode(bobConnection);

        if (aliceFingerprint != null) {
            PubSubDelegate.deletePublicKeyNode(aliceConnection, aliceFingerprint);
        }
        if (bobFingerprint != null) {
            PubSubDelegate.deletePublicKeyNode(bobConnection, bobFingerprint);
        }
    }
}
