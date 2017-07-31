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
package org.jivesoftware.smackx.jingle.transport;

import static junit.framework.TestCase.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.logging.Level;

import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.components.JingleContent;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.components.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle.util.Role;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.Assert;

/**
 * Test the JingleIBBTransport in a very basic case.
 */
public class JingleTransportTest extends AbstractSmackIntegrationTest {

    public JingleTransportTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void JingleIBBTest() throws Exception {
        JingleIBBTransport sender = new JingleIBBTransport();
        final JingleIBBTransport receiver = new JingleIBBTransport(sender.getSid(), sender.getBlockSize());

        basicTransportTest(sender, receiver);
    }

    @SmackIntegrationTest
    public void JingleS5BTest() throws Exception {
        JingleSession sSession = new JingleSession(JingleManager.getInstanceFor(conOne), conOne.getUser().asFullJidOrThrow(), conTwo.getUser().asFullJidOrThrow(), Role.initiator, "session");
        JingleSession rSession = new JingleSession(JingleManager.getInstanceFor(conTwo), conTwo.getUser().asFullJidOrThrow(), conOne.getUser().asFullJidOrThrow(), Role.responder, "session");
        JingleContent sContent = new JingleContent(null, null, null, "content", null, JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleContent rContent = new JingleContent(null, null, null, "content", null, JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        sSession.addContent(sContent);
        rSession.addContent(rContent);
        JingleS5BTransport sender = (JingleS5BTransport) JingleS5BTransportManager.getInstanceFor(conOne).createTransport(sContent);
        JingleS5BTransport receiver = (JingleS5BTransport) JingleS5BTransportManager.getInstanceFor(conTwo).createTransport(rContent, sender);

        basicTransportTest(sender, receiver);
    }


    public void basicTransportTest(final JingleTransport<?> sender, final JingleTransport<?> receiver) throws Exception {
        JingleSession senderSession = new JingleSession(null, conTwo.getUser().asFullJidOrThrow(), conOne.getUser().asFullJidOrThrow(), Role.initiator, "sid");
        JingleSession receiverSession = new JingleSession(null, conTwo.getUser().asFullJidOrThrow(), conOne.getUser().asFullJidOrThrow(), Role.responder, "sid");

        final SimpleResultSyncPoint recvPoint = new SimpleResultSyncPoint();

        final int size = 16000;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final byte[] recv = new byte[size];

        receiver.establishIncomingBytestreamSession(conOne, new JingleTransportCallback() {
            @Override
            public void onTransportReady(final BytestreamSession bytestreamSession) {
                LOGGER.log(Level.INFO, "Receiving!");
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream inputStream = bytestreamSession.getInputStream();

                            byte[] buf = new byte[512];
                            int read = 0;
                            while (read < size) {
                                int r = inputStream.read(buf);
                                if (r >= 0) {
                                    System.arraycopy(buf, 0, recv, read, r);
                                    read += r;
                                } else {
                                    break;
                                }
                            }

                            LOGGER.log(Level.INFO, "Success!");

                            bytestreamSession.getInputStream().close();
                            recvPoint.signal();
                        } catch (IOException e) {
                            fail(e.toString());
                        }
                    }
                });
            }

            @Override
            public void onTransportFailed(Exception e) {
                LOGGER.log(Level.SEVERE, e.toString());
                recvPoint.signal();
            }
        }, receiverSession);

        sender.establishOutgoingBytestreamSession(conTwo, new JingleTransportCallback() {
            @Override
            public void onTransportReady(final BytestreamSession bytestreamSession) {
                LOGGER.log(Level.INFO, "Sending!");
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream outputStream = bytestreamSession.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();

                        } catch (IOException e) {
                            fail(e.toString());
                        }
                    }
                });
            }

            @Override
            public void onTransportFailed(Exception e) {
                LOGGER.log(Level.SEVERE, e.toString());
            }
        }, senderSession);

        recvPoint.waitForResult(60 * 1000);
        Assert.assertArrayEquals(data, recv);
    }
}
