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
package org.jivesoftware.smackx.jingle_filetransfer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.handler.FileTransferHandler;
import org.jivesoftware.smackx.jingle_filetransfer.listener.JingleFileTransferOfferListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.AfterClass;
import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 29.06.17.
 */
public class FileTransferTest extends AbstractSmackIntegrationTest {

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

    public FileTransferTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void basicFileTransferTest() {
        final SimpleResultSyncPoint resultSyncPoint1 = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint resultSyncPoint2 = new SimpleResultSyncPoint();

        FullJid alice = conOne.getUser().asFullJidOrThrow();
        FullJid bob = conTwo.getUser().asFullJidOrThrow();

        File source = prepareNewTestFile("source");
        final File target = new File(tempDir, "target");

        JingleFileTransferManager aftm = JingleFileTransferManager.getInstanceFor(conOne);
        JingleFileTransferManager bftm = JingleFileTransferManager.getInstanceFor(conTwo);

        bftm.addJingleFileTransferOfferListener(new JingleFileTransferOfferListener() {
            @Override
            public void onFileOffer(JingleElement request, IncomingFileOfferCallback callback) {
                FileTransferHandler handler2 = callback.acceptIncomingFileOffer(request, target);
                handler2.addEndedListener(new FileTransferHandler.EndedListener() {
                    @Override
                    public void onEnded(JingleReasonElement.Reason reason) {
                        resultSyncPoint2.signal();
                    }
                });
            }
        });

        try {
            FileTransferHandler handler = aftm.sendFile(bob, source);
            handler.addEndedListener(new FileTransferHandler.EndedListener() {
                @Override
                public void onEnded(JingleReasonElement.Reason reason) {
                    resultSyncPoint1.signal();
                }
            });
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            fail();
        }

        try {
            resultSyncPoint1.waitForResult(10 * 1000);
        } catch (Exception e) {
            fail();
        }

        try {
            resultSyncPoint2.waitForResult(10 * 1000);
        } catch (Exception e) {
            fail();
        }

        byte[] sBytes = new byte[(int) source.length()];
        byte[] tBytes = new byte[(int) target.length()];
        try {
            FileInputStream fi = new FileInputStream(source);
            fi.read(sBytes);
            fi.close();
            fi = new FileInputStream(target);
            fi.read(tBytes);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read files.");
            fail();
        }

        assertArrayEquals(sBytes, tBytes);
        LOGGER.log(Level.INFO, "SUCCESSFULLY SENT AND RECEIVED");

    }

    private File prepareNewTestFile(String name) {
        File testFile = new File(tempDir, name);
        try {
            if (!testFile.exists()) {
                testFile.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(testFile);
            byte[] rand = new byte[16000];
            INSECURE_RANDOM.nextBytes(rand);
            fo.write(rand);
            fo.close();
            return testFile;
        } catch (IOException e) {
            return null;
        }
    }

    @AfterClass
    public void cleanup() {
        Socks5Proxy.getSocks5Proxy().stop();
    }
}
