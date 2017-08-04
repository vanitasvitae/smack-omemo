package org.jivesoftware.smackx.jingle.component;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.util.Role;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class JingleSessionTest extends SmackTestSuite {

    @Test
    public void jingleSessionTest() throws XmppStringprepException {
        DummyConnection dummyConnection = new DummyConnection();
        FullJid alice = JidCreate.fullFrom("alice@wonderland.lit/test123");
        FullJid madHatter = JidCreate.fullFrom("mad@hat.net/cat");

        JingleManager jingleManager = JingleManager.getInstanceFor(dummyConnection);

        JingleSession session = new JingleSession(jingleManager, alice, madHatter, Role.initiator, "WeReAlLmAdHeRe");

        assertEquals(alice, session.getInitiator());
        assertEquals(madHatter, session.getResponder());
        assertEquals(alice, session.getOurJid());
        assertEquals(madHatter, session.getPeer());

        assertEquals(0, session.getContents().size());
        assertEquals("WeReAlLmAdHeRe", session.getSessionId());
        assertEquals(jingleManager, session.getJingleManager());
    }

    @Test(expected = IllegalStateException.class)
    public void getSoleContentThrowingTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()), null, null, Role.initiator, null);
        assertTrue(session.isInitiator());
        assertFalse(session.isResponder());
        JingleContent c1 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleContent c2 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        session.addContent(c1);
        assertEquals(c1, session.getContent(c1.getName()));
        session.addContent(c2);
        assertEquals(c2, session.getContent(c2.getName()));

        session.getSoleContentOrThrow();
    }

    @Test
    public void getSoleContentTest() {
        JingleSession session = new JingleSession(JingleManager.getInstanceFor(new DummyConnection()), null, null, Role.responder, null);
        assertTrue(session.isResponder());
        assertFalse(session.isInitiator());
        assertNull(session.getSoleContentOrThrow());
        JingleContent c1 = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        assertNull(c1.getParent());
        session.addContent(c1);
        assertEquals(session, c1.getParent());

        assertEquals(c1, session.getSoleContentOrThrow());
    }
}
