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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

/**
 * Simple OMEMO message encryption integration test.
 * During this test Alice sends an encrypted message to Bob. Bob decrypts it and sends a response to Alice.
 * It is checked whether the messages can be decrypted, and if used up pre-keys result in renewed bundles.
 */
public class MessageEncryptionIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public MessageEncryptionIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @SmackIntegrationTest
    public void messageTest() throws Exception {
        OmemoBundleElement aliceBundle1 = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement bobsBundle1 = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());

        final String message1 = "One is greater than zero (for small values of zero).";
        OmemoMessage.Sent encrypted1 = alice.encrypt(bob.getOwnJid(), message1);
        final SimpleResultSyncPoint bobReceivedMessage = new SimpleResultSyncPoint();

        bob.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
                if (received.getMessage().equals(message1)) {
                    bobReceivedMessage.signal();
                } else {
                    bobReceivedMessage.signalFailure("Received decrypted message was not equal to sent message.");
                }
            }

            @Override
            public void onOmemoKeyTransportReceived(Stanza stanza, OmemoMessage.Received decryptedKeyTransportMessage) {
                // Not needed
            }

        });

        Message m1 = new Message();
        m1.addExtension(encrypted1.getElement());
        m1.setTo(bob.getOwnJid());
        alice.getConnection().sendStanza(m1);
        bobReceivedMessage.waitForResult(10 * 1000);

        OmemoBundleElement aliceBundle2 = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement bobsBundle2 = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());

        // Alice bundle is still the same, but bobs bundle changed, because he used up a pre-key.
        assertEquals(aliceBundle1, aliceBundle2);
        assertFalse(bobsBundle1.equals(bobsBundle2));

        final String message2 = "The german words for 'leek' and 'wimp' are the same.";
        final OmemoMessage.Sent encrypted2 = bob.encrypt(alice.getOwnJid(), message2);
        final SimpleResultSyncPoint aliceReceivedMessage = new SimpleResultSyncPoint();

        alice.addOmemoMessageListener(new OmemoMessageListener() {
            @Override
            public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
                if (received.getMessage().equals(message2)) {
                    aliceReceivedMessage.signal();
                } else {
                    aliceReceivedMessage.signalFailure("Received decrypted message was not equal to sent message.");
                }
            }

            @Override
            public void onOmemoKeyTransportReceived(Stanza stanza, OmemoMessage.Received decryptedKeyTransportMessage) {
                // Not needed
            }
        });

        Message m2 = new Message();
        m2.addExtension(encrypted2.getElement());
        m2.setTo(alice.getOwnJid());
        bob.getConnection().sendStanza(m2);
        aliceReceivedMessage.waitForResult(10 * 1000);

        OmemoBundleElement aliceBundle3 = alice.getOmemoService().getOmemoStoreBackend().packOmemoBundle(alice.getOwnDevice());
        OmemoBundleElement bobsBundle3 = bob.getOmemoService().getOmemoStoreBackend().packOmemoBundle(bob.getOwnDevice());

        // Alice bundle did not change, because she already has a session with bob, which he initiated.
        // Bobs bundle doesn't change this time.
        assertEquals(aliceBundle2, aliceBundle3);
        assertEquals(bobsBundle2, bobsBundle3);
    }
}
