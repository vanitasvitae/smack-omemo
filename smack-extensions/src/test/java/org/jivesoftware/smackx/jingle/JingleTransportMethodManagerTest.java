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
import static junit.framework.TestCase.assertNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleAction;
import org.jivesoftware.smackx.jingle3.element.JingleContentElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle3.transport.legacy.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle3.transport.jingle_s5b.elements.JingleS5BTransportElement;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class JingleTransportMethodManagerTest extends SmackTestSuite {

    @Test
    public void getTransportManagerTest() throws XmppStringprepException {
        XMPPConnection connection = new DummyConnection();
        JingleTransportMethodManager jtmm = JingleTransportMethodManager.getInstanceFor(connection);

        assertNull(jtmm.getBestAvailableTransportManager());
        assertNull(jtmm.getTransportManager(JingleIBBTransport.NAMESPACE_V1));
        assertNull(jtmm.getTransportManager(JingleS5BTransportElement.NAMESPACE_V1));

        jtmm.registerTransportManager(JingleIBBTransportManager.getInstanceFor(connection));
        assertNull(jtmm.getTransportManager(JingleS5BTransportElement.NAMESPACE_V1));
        assertNotNull(jtmm.getTransportManager(JingleIBBTransport.NAMESPACE_V1));
        assertEquals(JingleIBBTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager());

        jtmm.registerTransportManager(JingleS5BTransportManager.getInstanceFor(connection));
        assertEquals(JingleS5BTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager());

        jtmm.unregisterTransportManager(JingleS5BTransportManager.getInstanceFor(connection));
        assertNull(jtmm.getTransportManager(JingleS5BTransportElement.NAMESPACE_V1));
        jtmm.unregisterTransportManager(JingleIBBTransportManager.getInstanceFor(connection));

        assertNull(jtmm.getBestAvailableTransportManager());

        jtmm.registerTransportManager(JingleS5BTransportManager.getInstanceFor(connection));
        assertEquals(JingleS5BTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager());
        jtmm.registerTransportManager(JingleIBBTransportManager.getInstanceFor(connection));
        assertEquals(JingleS5BTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager());

        assertEquals(JingleIBBTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager(
                Collections.singleton(JingleS5BTransportElement.NAMESPACE_V1)));

        JingleStubTransportManager stub = new JingleStubTransportManager(connection);
        jtmm.registerTransportManager(stub);
        assertEquals(stub, JingleTransportMethodManager.getTransportManager(connection, JingleStubTransportManager.NAMESPACE));
        assertEquals(JingleS5BTransportManager.getInstanceFor(connection), jtmm.getBestAvailableTransportManager());

        JingleElement jingle = JingleElement.getBuilder().setSessionId("test").setAction(JingleAction.session_initiate)
                .setInitiator(JidCreate.fullFrom("test@test.test/test"))
                .addJingleContent(
                        JingleContentElement.getBuilder().setCreator(JingleContentElement.Creator.initiator).setName("content")
                                .setSenders(JingleContentElement.Senders.initiator).setTransport(
                                        new JingleIBBTransport("transportId")).build()).build();
        assertEquals(JingleIBBTransportManager.getInstanceFor(connection), jtmm.getTransportManager(jingle));
        assertEquals(JingleIBBTransportManager.getInstanceFor(connection),
                JingleTransportMethodManager.getTransportManager(connection, jingle));

        Set<String> except = new HashSet<>();
        except.add(JingleIBBTransport.NAMESPACE_V1);
        except.add(JingleS5BTransportElement.NAMESPACE_V1);
        assertEquals(stub, jtmm.getBestAvailableTransportManager(except));

        jtmm.unregisterTransportManager(JingleS5BTransportManager.getInstanceFor(connection));
        jtmm.unregisterTransportManager(JingleIBBTransportManager.getInstanceFor(connection));
        assertEquals(stub, JingleTransportMethodManager.getBestAvailableTransportManager(connection));
    }

    private static class JingleStubTransportManager extends JingleTransportManager<JingleContentTransportElement> {

        public static final String NAMESPACE = "urn:xmpp:jingle:transports:stub:0";

        public JingleStubTransportManager(XMPPConnection connection) {
            super(connection);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public JingleTransportSession<JingleContentTransportElement> transportSession(JingleSession jingleSession) {
            return null;
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {

        }
    }
}
