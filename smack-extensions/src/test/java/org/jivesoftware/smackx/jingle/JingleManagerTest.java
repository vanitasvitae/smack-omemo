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
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.element.Jingle;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Created by vanitas on 03.07.17.
 */
public class JingleManagerTest extends SmackTestSuite {

    /**
     * Might fail in *very* rare cases.
     */
    @Test
    public void randomTest() {
        String r1 = JingleManager.randomId();
        String r2 = JingleManager.randomId();

        assertNotSame(r1, r2);
        assertEquals(24, r1.length());
    }

    @Test
    public void threadPoolTest() {
        assertNotNull(JingleManager.getThreadPool());
    }

    @Test
    public void handlerRegistrationTest() throws XmppStringprepException {
        final XMPPConnection connection = new DummyConnection();

        FullJid remote = JidCreate.fullFrom("test@test.test/test");
        FullJid local = JidCreate.fullFrom("case@case.case/case");
        String sid = JingleManager.randomId();
        JingleSession s = new JingleSession(local, remote, Role.initiator, sid) {
            @Override
            public XMPPConnection getConnection() {
                return connection;
            }

            @Override
            public void onTransportMethodFailed(String namespace) {

            }
        };
        assertNull(JingleManager.getInstanceFor(connection).registerJingleSessionHandler(remote, sid, s));
        assertNotNull(JingleManager.getInstanceFor(connection).registerJingleSessionHandler(remote, sid, s));
        JingleManager.getInstanceFor(connection).unregisterJingleSessionHandler(remote, sid, s);
        assertNull(JingleManager.getInstanceFor(connection).registerJingleSessionHandler(remote, sid, s));

        String stubNamespace = "urn:xmpp:jingle:application:stub:0";
        JingleHandler stub = new JingleHandler() {
            @Override
            public IQ handleJingleRequest(Jingle jingle) {
                return null;
            }
        };

        assertNull(JingleManager.getInstanceFor(connection).registerDescriptionHandler(stubNamespace, stub));
        assertNotNull(JingleManager.getInstanceFor(connection).registerDescriptionHandler(stubNamespace, stub));
    }
}
