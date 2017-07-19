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
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;

import java.util.List;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.TestIQ;
import org.jivesoftware.smackx.jingle3.FullJidAndSessionId;
import org.jivesoftware.smackx.jingle3.element.JingleContentElement;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleAction;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.JingleIBBTransportSession;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class JingleSessionTest {

    private static final XMPPConnection connection = new DummyConnection();

    private static final IQ sessionInitiateResult = new TestIQ();
    private static final IQ sessionTerminateResult = new TestIQ();
    private static final IQ sessionInfoResult = new TestIQ();
    private static final IQ sessionAcceptResult = new TestIQ();

    private static final IQ contentAddResult = new TestIQ();
    private static final IQ contentAcceptResult = new TestIQ();
    private static final IQ contentRejectResult = new TestIQ();
    private static final IQ contentModifyResult = new TestIQ();
    private static final IQ contentRemoveResult = new TestIQ();

    private static final IQ descriptionInfoResult = new TestIQ();

    private static final IQ securityInfoResult = new TestIQ();

    private static final IQ transportAcceptResult = new TestIQ();
    private static final IQ transportReplaceResult = new TestIQ();
    private static final IQ transportRejectResult = new TestIQ();

    @Test
    public void jingleSessionTest() throws XmppStringprepException {
        FullJid us5 = JidCreate.fullFrom("home@swe.et/home");
        FullJid u2 = JidCreate.fullFrom("place@far.far/away");
        String sessionId = "suchPopMuchWow";

        JingleSession initiatedSimpleSession = new SimpleSession(us5, u2, Role.initiator, sessionId);
        assertEquals(us5, initiatedSimpleSession.getInitiator());
        assertEquals(u2, initiatedSimpleSession.getResponder());
        assertEquals(us5, initiatedSimpleSession.getLocal());
        assertEquals(u2, initiatedSimpleSession.getRemote());
        assertEquals(sessionId, initiatedSimpleSession.getSessionId());
        assertNotNull(initiatedSimpleSession.getContents());

        String sessionId2 = "popMusicSucks";
        JingleSession respondedSimpleSession = new SimpleSession(u2, us5, Role.responder, sessionId2);
        assertEquals(us5, respondedSimpleSession.getLocal());
        assertEquals(us5, respondedSimpleSession.getResponder());
        assertEquals(u2, respondedSimpleSession.getInitiator());
        assertEquals(u2, respondedSimpleSession.getRemote());

        assertEquals(new FullJidAndSessionId(u2, sessionId), initiatedSimpleSession.getFullJidAndSessionId());
        assertEquals(new FullJidAndSessionId(u2, sessionId2), respondedSimpleSession.getFullJidAndSessionId());

        assertNull(initiatedSimpleSession.getTransportSession());
        initiatedSimpleSession.setTransportSession(new JingleIBBTransportSession(initiatedSimpleSession));
        assertNotNull(initiatedSimpleSession.getTransportSession());

        assertNotSame(initiatedSimpleSession, respondedSimpleSession);
        assertFalse(initiatedSimpleSession.equals(respondedSimpleSession));
        assertNotSame(initiatedSimpleSession.hashCode(), respondedSimpleSession.hashCode());
        assertFalse(initiatedSimpleSession.equals("Hallo Welt"));
    }

    @Test
    public void testHandleSessionRequest() {
        JingleSession s = new SimpleSession(null, null, null, null);

        assertEquals(sessionAcceptResult, s.handleJingleSessionRequest(simpleAction(JingleAction.session_accept)));
        assertEquals(sessionInfoResult, s.handleJingleSessionRequest(simpleAction(JingleAction.session_info)));
        assertEquals(sessionInitiateResult, s.handleJingleSessionRequest(simpleAction(JingleAction.session_initiate)));
        assertEquals(sessionTerminateResult, s.handleJingleSessionRequest(simpleAction(JingleAction.session_terminate)));

        assertEquals(contentAcceptResult, s.handleJingleSessionRequest(simpleAction(JingleAction.content_accept)));
        assertEquals(contentAddResult, s.handleJingleSessionRequest(simpleAction(JingleAction.content_add)));
        assertEquals(contentModifyResult, s.handleJingleSessionRequest(simpleAction(JingleAction.content_modify)));
        assertEquals(contentRejectResult, s.handleJingleSessionRequest(simpleAction(JingleAction.content_reject)));
        assertEquals(contentRemoveResult, s.handleJingleSessionRequest(simpleAction(JingleAction.content_remove)));

        assertEquals(descriptionInfoResult, s.handleJingleSessionRequest(simpleAction(JingleAction.description_info)));

        assertEquals(securityInfoResult, s.handleJingleSessionRequest(simpleAction(JingleAction.security_info)));

        assertEquals(transportAcceptResult, s.handleJingleSessionRequest(simpleAction(JingleAction.transport_accept)));
        assertEquals(transportRejectResult, s.handleJingleSessionRequest(simpleAction(JingleAction.transport_reject)));
        assertEquals(transportReplaceResult, s.handleJingleSessionRequest(simpleAction(JingleAction.transport_replace)));
    }

    private static class SimpleSession extends JingleSession {

        public SimpleSession(FullJid initiator, FullJid responder, Role role, String sid) {
            super(initiator, responder, role, sid);
        }

        public SimpleSession(FullJid initiator, FullJid responder, Role role, String sid, List<JingleContentElement> contents) {
            super(initiator, responder, role, sid, contents);
        }

        @Override
        public XMPPConnection getConnection() {
            return connection;
        }

        @Override
        public void onTransportMethodFailed(String namespace) {

        }

        @Override
        public IQ handleSessionInitiate(JingleElement jingle) {
            return sessionInitiateResult;
        }

        @Override
        public IQ handleSessionAccept(JingleElement jingle) {
            return sessionAcceptResult;
        }

        @Override
        public IQ handleSessionTerminate(JingleElement jingle) {
            return sessionTerminateResult;
        }

        @Override
        public IQ handleSessionInfo(JingleElement jingle) {
            return sessionInfoResult;
        }

        @Override
        public IQ handleTransportAccept(JingleElement jingle) {
            return transportAcceptResult;
        }

        @Override
        public IQ handleTransportReject(JingleElement jingle) {
            return transportRejectResult;
        }

        @Override
        public IQ handleTransportReplace(JingleElement jingle) {
            return transportReplaceResult;
        }

        @Override
        public IQ handleContentAdd(JingleElement jingle) {
            return contentAddResult;
        }

        @Override
        public IQ handleContentAccept(JingleElement jingle) {
            return contentAcceptResult;
        }

        @Override
        public IQ handleContentReject(JingleElement jingle) {
            return contentRejectResult;
        }

        @Override
        public IQ handleContentRemove(JingleElement jingle) {
            return contentRemoveResult;
        }

        @Override
        public IQ handleContentModify(JingleElement jingle) {
            return contentModifyResult;
        }

        @Override
        public IQ handleDescriptionInfo(JingleElement jingle) {
            return descriptionInfoResult;
        }

        @Override
        public IQ handleSecurityInfo(JingleElement jingle) {
            return securityInfoResult;
        }
    }

    private JingleElement simpleAction(JingleAction action) {
        return JingleElement.getBuilder().setAction(action).setSessionId("test").build();
    }
}
