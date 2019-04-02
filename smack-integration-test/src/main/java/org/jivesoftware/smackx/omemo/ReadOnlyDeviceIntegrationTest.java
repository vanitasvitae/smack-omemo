/**
 *
 * Copyright 2018 Paul Schaub
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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.ReadOnlyDeviceException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

public class ReadOnlyDeviceIntegrationTest extends AbstractTwoUsersOmemoIntegrationTest {

    public ReadOnlyDeviceIntegrationTest(SmackIntegrationTestEnvironment<?> environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @SmackIntegrationTest
    public void test() throws InterruptedException, SmackException.NoResponseException, SmackException.NotLoggedInException, SmackException.NotConnectedException, CryptoFailedException, UndecidedOmemoIdentityException, CorruptedOmemoKeyException, NoSuchAlgorithmException, CannotEstablishOmemoSessionException, NoRawSessionException {
        int allowedChainLength = 5;
        boolean prevIgnoreReadOnlyConf = OmemoConfiguration.getIgnoreReadOnlyDevices();
        int prevMaxMessageCounter = OmemoConfiguration.getMaxReadOnlyMessageCount();

        OmemoConfiguration.setIgnoreReadOnlyDevices(true);
        // Set the maxReadOnlyMessageCount to ridiculously low threshold of 5.
        // This means that Alice will be able to encrypt 5 messages for Bob, while the 6th will not be encrypted for Bob.
        OmemoConfiguration.setMaxReadOnlyMessageCount(allowedChainLength);

        // Reset counter to begin test
        alice.getOmemoService().buildFreshSessionWithDevice(alice.getConnection(), alice.getOwnDevice(), bob.getOwnDevice());
        assertChainLengthEquals(alice, bob, 0);

        // Since the max threshold is allowedChainLength, we must be able to encrypt as many messages for Bob.
        for (int i = 0; i < allowedChainLength; i++) {
            OmemoMessage.Sent message = alice.encrypt(bob.getOwnJid(), "Hello World!");
            assertFalse(message.getSkippedDevices().containsKey(bob.getOwnDevice()));
            assertChainLengthEquals(alice, bob, i + 1);
        }

        // Now the message counter must be too high and Bobs device must be skipped.
        OmemoMessage.Sent message = alice.encrypt(bob.getOwnJid(), "Hello World!");
        assertChainLengthEquals(alice, bob, allowedChainLength);
        assertTrue(message.getSkippedDevices().containsKey(bob.getOwnDevice()));
        Throwable exception = message.getSkippedDevices().get(bob.getOwnDevice());
        assertTrue(exception instanceof ReadOnlyDeviceException);
        assertEquals(bob.getOwnDevice(), ((ReadOnlyDeviceException) exception).getDevice());

        // Reset the configuration to previous values
        OmemoConfiguration.setMaxReadOnlyMessageCount(prevMaxMessageCounter);
        OmemoConfiguration.setIgnoreReadOnlyDevices(prevIgnoreReadOnlyConf);
    }

    private void assertChainLengthEquals(OmemoManager sender, OmemoManager receiver, int length) throws NoRawSessionException {
        assertEquals(length, sender.getOmemoService().getOmemoStoreBackend()
                .getLengthOfSessionSendingChain(sender.getOwnDevice(), receiver.getOwnDevice()));
    }
}
