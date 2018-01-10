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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

public class SessionRenegotiationIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public SessionRenegotiationIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @SmackIntegrationTest
    public void sessionRenegotiationTest() throws Exception {

        if (!OmemoConfiguration.getRepairBrokenSessionsWithPreKeyMessages()) {
            throw new TestNotPossibleException("This test requires the property " +
                    "OmemoConfiguration.REPAIR_BROKEN_SESSIONS_WITH_PREKEY_MESSAGES " +
                    "set to 'true'.");
        }

        // send PreKeyMessage -> Success
        final String body1 = "P = NP is true for all N,P from the set of complex numbers, where P is equal to 0";
        AbstractOmemoMessageListener.PreKeyMessageListener listener1 =
                new AbstractOmemoMessageListener.PreKeyMessageListener(body1);
        OmemoMessage.Sent e1 = alice.encrypt(bob.getOwnJid(), body1);
        bob.addOmemoMessageListener(listener1);
        alice.getConnection().sendStanza(e1.asMessage(bob.getOwnJid()));
        listener1.getSyncPoint().waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(listener1);

        // Remove the session on Bobs side.
        synchronized (bob.LOCK) {
            bob.getOmemoService().getOmemoStoreBackend().removeRawSession(bob.getOwnDevice(), alice.getOwnDevice());
        }

        // Send normal message -> fail, bob repairs session with preKeyMessage
        final String body3 = "P = NP is also true for all N,P from the set of complex numbers, where N is equal to 1.";
        AbstractOmemoMessageListener.PreKeyKeyTransportListener listener3 =
                new AbstractOmemoMessageListener.PreKeyKeyTransportListener();
        OmemoMessage.Sent e3 = alice.encrypt(bob.getOwnJid(), body3);
        alice.addOmemoMessageListener(listener3);
        alice.getConnection().sendStanza(e3.asMessage(bob.getOwnJid()));
        listener3.getSyncPoint().waitForResult(10 * 1000);
        alice.removeOmemoMessageListener(listener3);

        // Send normal message -> success
        final String body4 = "P = NP would be a disaster for the world of cryptography.";
        AbstractOmemoMessageListener.MessageListener listener4 = new AbstractOmemoMessageListener.MessageListener(body4);
        OmemoMessage.Sent e4 = alice.encrypt(bob.getOwnJid(), body4);
        bob.addOmemoMessageListener(listener4);
        alice.getConnection().sendStanza(e4.asMessage(bob.getOwnJid()));
        listener4.getSyncPoint().waitForResult(10 * 1000);
        bob.removeOmemoMessageListener(listener4);
    }
}
