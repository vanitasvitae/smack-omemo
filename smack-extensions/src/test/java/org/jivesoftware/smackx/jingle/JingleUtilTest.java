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
package org.jivesoftware.smackx.jingle;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;

import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;


/**
 * Test the JingleUtil class.
 */
public class JingleUtilTest extends SmackTestSuite {

    private XMPPConnection connection;
    private JingleUtil jutil;

    private FullJid romeo;
    private FullJid juliet;

    @Before
    public void setup() throws XmppStringprepException {

        connection = new DummyConnection(
                DummyConnection.getDummyConfigurationBuilder()
                .setUsernameAndPassword("romeo@montague.lit",
                "iluvJulibabe13").build());
        jutil = new JingleUtil(connection);
        romeo = connection.getUser().asFullJidOrThrow();
        juliet = JidCreate.fullFrom("juliet@capulet.lit/balcony");
    }

    @Test
    public void createAckTest() {
        Jingle jingle = Jingle.getBuilder().setAction(JingleAction.session_initiate).setInitiator(romeo).setSessionId("test").build();
        IQ result = jutil.createAck(jingle);
        assertEquals(jingle.getStanzaId(), result.getStanzaId());
        assertTrue(result.getType() == IQ.Type.result);
    }
}
