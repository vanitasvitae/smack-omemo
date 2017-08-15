/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet;

import static org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferIntegrationTest.prepareNewTestFile;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.subscribe;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.unidirectionalTrust;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;
import org.jivesoftware.smackx.omemo.AbstractOmemoIntegrationTest;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.provider.OmemoVAxolotlProvider;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

public class JetIntegrationTest extends AbstractOmemoIntegrationTest {

    private OmemoManager oa, ob;
    private JetManager ja, jb;
    private JingleIBBTransportManager ia, ib;
    private OmemoStore<?,?,?,?,?,?,?,?,?> store;

    private static final File tempDir;

    static {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            tempDir = new File(f, ".config/smack-integration-test/");
        } else {
            tempDir = new File("int_test_jingle");
        }
    }

    public JetIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @Override
    public void before() {
        store = OmemoService.getInstance().getOmemoStoreBackend();
        oa = OmemoManager.getInstanceFor(conOne, 666);
        ob = OmemoManager.getInstanceFor(conTwo, 777);
        ja = JetManager.getInstanceFor(conOne);
        jb = JetManager.getInstanceFor(conTwo);
        ia = JingleIBBTransportManager.getInstanceFor(conOne);
        ib = JingleIBBTransportManager.getInstanceFor(conTwo);
        JetManager.registerEncryptionMethodProvider(OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL, new OmemoVAxolotlProvider());
    }

    @SmackIntegrationTest
    public void JingleEncryptedFileTransferTest()
            throws Exception {

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        //Setup OMEMO
        subscribe(oa, ob, "Bob");
        subscribe(ob, oa, "Alice");
        setUpOmemoManager(oa);
        setUpOmemoManager(ob);
        unidirectionalTrust(oa, ob);
        unidirectionalTrust(ob, oa);

        ja.registerEncryptionMethod(oa);
        jb.registerEncryptionMethod(ob);

        File source = prepareNewTestFile("source");
        final File target = new File(tempDir, "target");

        JingleFileTransferManager.getInstanceFor(conTwo).addIncomingFileOfferListener(new IncomingFileOfferListener() {
            @Override
            public void onIncomingFileOffer(IncomingFileOfferController offer) {
                try {
                    offer.addProgressListener(new ProgressListener() {
                        @Override
                        public void started() {

                        }

                        @Override
                        public void progress(float percent) {

                        }

                        @Override
                        public void finished() {
                            received.signal();
                        }
                    });
                    offer.accept(conTwo, target);
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException | IOException e) {
                    received.signal(e);
                }
            }
        });

        ja.sendEncryptedFile(source, conTwo.getUser().asFullJidOrThrow(), oa);

        received.waitForResult(60 * 1000);

        FileInputStream sIn = new FileInputStream(source);
        FileInputStream tIn = new FileInputStream(target);

        byte[] sB = new byte[(int) source.length()];
        byte[] tB = new byte[(int) target.length()];

        sIn.read(sB);
        tIn.read(tB);

        assertArrayEquals(sB, tB);
    }

    @Override
    public void after() {
        oa.shutdown();
        ob.shutdown();
        cleanServerSideTraces(oa);
        cleanServerSideTraces(ob);
    }
}
