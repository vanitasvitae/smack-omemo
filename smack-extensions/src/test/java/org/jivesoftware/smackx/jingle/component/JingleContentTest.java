package org.jivesoftware.smackx.jingle.component;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;

import org.junit.Test;

public class JingleContentTest extends SmackTestSuite {

    @Test
    public void jingleContentTest() {
        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.responder);
        assertEquals(JingleContentElement.Creator.initiator, content.getCreator());
        assertEquals(JingleContentElement.Senders.responder, content.getSenders());
        assertNull(content.getDescription());
        assertNull(content.getTransport());
        assertNull(content.getSecurity());
        assertNotNull(content.getName()); //MUST NOT BE NULL!
        assertEquals(0, content.getTransportBlacklist().size());
    }
}
