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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.fail;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

public class SessionRenegotiationIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public SessionRenegotiationIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    private static final String body1 = "P = NP is true for all N,P from the set of complex numbers, where P is equal to 0";
    private final SimpleResultSyncPoint bsp1 = new SimpleResultSyncPoint();
    private final OmemoMessageListener bml1 = new OmemoTestMessageListener(body1, bsp1);

    private static final String body2 = "P = NP is also true for all N,P from the set of complex numbers, where N is equal to 1.";
    private final ResultSyncPoint<Boolean, IllegalStateException> bsp2 = new ResultSyncPoint<>();
    private final OmemoMessageListener bml2 = new OmemoMessageListener() {
        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            if (received.getMessage().equals(body2)) {
                bsp2.signal(new IllegalStateException("Message MUST NOT be decryptable!"));
            } else {
                bsp2.signal(new IllegalStateException("OmemoMessageListener MUST NOT be called for this message."));
            }
        }

        @Override
        public void onOmemoKeyTransportReceived(Stanza stanza, OmemoMessage.Received decryptedKeyTransportMessage) {
            // Not needed
        }
    };
    private final SimpleResultSyncPoint asp2 = new SimpleResultSyncPoint();
    private final OmemoMessageListener aml2 = new OmemoMessageListener() {

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received decryptedMessage) {
            // Not needed
        }

        @Override
        public void onOmemoKeyTransportReceived(Stanza stanza, OmemoMessage.Received received) {
            asp2.signal();
        }
    };

    private static final String body3 = "P = NP would be a disaster for the world of cryptography.";
    private final SimpleResultSyncPoint bsp3 = new SimpleResultSyncPoint();
    private final OmemoMessageListener bml3 = new OmemoTestMessageListener(body3, bsp3);

    @SmackIntegrationTest
    public void sessionRenegotiationTest() throws Exception {
        /*
        Send (PreKey-)message from Alice to Bob to initiate a session.
         */
        OmemoMessage.Sent e1 = alice.encrypt(bob.getOwnJid(), body1);
        Message m1 = new Message();
        m1.addExtension(e1.getElement());
        m1.setTo(bob.getOwnJid());

        bob.addOmemoMessageListener(bml1);
        alice.getConnection().sendStanza(m1);
        bsp1.waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(bml1);

        /*
        Delete the session records on Bobs side to render the session invalid.
         */
        bob.getOmemoService().getOmemoStoreBackend().removeRawSession(bob.getOwnDevice(), alice.getOwnDevice());

        /*
        Send normal message from Alice to Bob (Alice assumes, that Bob still has a valid session).
         */
        OmemoMessage.Sent e2 = alice.encrypt(bob.getOwnJid(), body2);
        Message m2 = new Message();
        m2.addExtension(e2.getElement());
        m2.setTo(bob.getOwnJid());

        bob.addOmemoMessageListener(bml2);
        alice.addOmemoMessageListener(aml2);
        alice.getConnection().sendStanza(m2);

        /*
        Wait for the timeout on Bobs side, since message decryption will fail now.
        Bob will respond with an empty PreKeyMessage though, in order to repair the session.
         */
        try {
            bsp2.waitForResult(10 * 1000);
            fail("This MUST throw a TimeoutException.");
        } catch (IllegalStateException e) {
            fail(e.getMessage());
        } catch (TimeoutException e) {
            // Expected.
        }
        asp2.waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(bml2);
        alice.removeOmemoMessageListener(aml2);

        /*
        Since Bob responded with a PreKeyMessage to repair the broken session, Alice should now be able to send messages
        which Bob can decrypt successfully again.
         */
        OmemoMessage.Sent e3 = alice.encrypt(bob.getOwnJid(), body3);
        Message m3 = new Message();
        m3.addExtension(e3.getElement());
        m3.setTo(bob.getOwnJid());

        bob.addOmemoMessageListener(bml3);
        alice.getConnection().sendStanza(m3);
        bsp3.waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(bml3);
    }

    private static class OmemoTestMessageListener implements OmemoMessageListener {

        private final String expectedMessage;
        private final SimpleResultSyncPoint syncPoint;

        OmemoTestMessageListener(String expectedMessage, SimpleResultSyncPoint syncPoint) {
            this.expectedMessage = expectedMessage;
            this.syncPoint = syncPoint;
        }

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            if (received.getMessage().equals(expectedMessage)) {
                syncPoint.signal();
            } else {
                syncPoint.signalFailure("Received decrypted message was not equal to sent message.");
            }
        }

        @Override
        public void onOmemoKeyTransportReceived(Stanza stanza, OmemoMessage.Received decryptedKeyTransportMessage) {

        }
    }
}
