package org.jivesoftware.smackx.jingle_ibb;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.jingle_ibb.element.JingleInBandByteStreamTransport;
import org.jivesoftware.smackx.jingle_ibb.provider.JingleInBandByteStreamTransportProvider;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

/**
 * Test JingleInBandByteStreamTransport provider and element.
 */
public class JingleInBandByteStreamTransportTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        String sid = JingleInBandByteStreamManager.generateSessionId();
        short size = 8192;

        String xml = "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' block-size='8192' sid='" + sid + "'/>";

        JingleInBandByteStreamTransport transport = new JingleInBandByteStreamTransport(size, sid);
        assertEquals(xml, transport.toXML().toString());
        assertEquals(size, transport.getBlockSize());
        assertEquals(sid, transport.getSessionId());

        JingleInBandByteStreamTransport parsed = new JingleInBandByteStreamTransportProvider()
                .parse(TestUtils.getParser(xml));
        assertEquals(transport, parsed);
        assertTrue(transport.equals(parsed));
        assertEquals(xml, parsed.toXML().toString());

        JingleInBandByteStreamTransport transport1 = new JingleInBandByteStreamTransport((short) 1024);
        assertEquals((short) 1024, transport1.getBlockSize());
        assertNotSame(transport, transport1);
        assertNotSame(transport.getSessionId(), transport1.getSessionId());

        assertFalse(transport.equals(null));

        JingleInBandByteStreamTransport transport2 = new JingleInBandByteStreamTransport();
        assertEquals(JingleInBandByteStreamTransport.DEFAULT_BLOCK_SIZE, transport2.getBlockSize());
        assertFalse(transport1.equals(transport2));

        JingleInBandByteStreamTransport transport3 = new JingleInBandByteStreamTransport((short) -1024);
        assertEquals(JingleInBandByteStreamTransport.DEFAULT_BLOCK_SIZE, transport3.getBlockSize());
    }
}
