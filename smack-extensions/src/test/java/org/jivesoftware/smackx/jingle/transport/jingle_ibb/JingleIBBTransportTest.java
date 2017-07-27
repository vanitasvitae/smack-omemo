/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.provider.JingleIBBTransportProvider;

import org.junit.Test;

/**
 * Test JingleIBBTransport provider and element.
 */
public class JingleIBBTransportTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        String sid = StringUtils.randomString(24);
        short size = 8192;

        String xml = "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' block-size='8192' sid='" + sid + "'/>";

        JingleIBBTransport transport = new JingleIBBTransport(sid, size);
        assertEquals(xml, transport.getElement().toXML().toString());
        assertEquals(size, (short) transport.getBlockSize());
        assertEquals(sid, transport.getSid());

        JingleIBBTransportElement parsed = new JingleIBBTransportProvider()
                .parse(TestUtils.getParser(xml));
        assertEquals(transport.getElement(), parsed);
        assertTrue(transport.getElement().equals(parsed));
        assertEquals(xml, parsed.toXML().toString());
        /*
        JingleIBBTransport transport1 = new JingleIBBTransport((short) 1024);
        assertEquals((short) 1024, transport1.getBlockSize());
        assertNotSame(transport, transport1);
        assertNotSame(transport.getSessionId(), transport1.getSessionId());

        assertFalse(transport.equals(null));

        JingleIBBTransport transport2 = new JingleIBBTransport();
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport2.getBlockSize());
        assertFalse(transport1.equals(transport2));

        JingleIBBTransport transport3 = new JingleIBBTransport((short) -1024);
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport3.getBlockSize());

        assertEquals(transport3.getNamespace(), JingleIBBTransport.NAMESPACE_V1);
        assertEquals(transport3.getElementName(), "transport");

        JingleIBBTransport transport4 = new JingleIBBTransport("session-id");
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport4.getBlockSize());
        */
    }
}
