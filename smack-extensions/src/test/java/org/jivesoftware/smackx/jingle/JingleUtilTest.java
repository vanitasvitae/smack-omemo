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
import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.provider.JingleProvider;

import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xml.sax.SAXException;


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
        JingleManager jm = JingleManager.getInstanceFor(connection);
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

    @Test
    public void createSessionTerminateDeclineTest() throws Exception {
        Jingle terminate = jutil.createSessionTerminateDecline(juliet, "thisismadness");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisismadness'>" +
                        "<reason>" +
                        "<decline/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, terminate.getStanzaId(), jingleXML);
        assertXMLEqual(xml, terminate.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.decline);
    }

    @Test
    public void createSessionTerminateSuccessTest() throws Exception {
        Jingle success = jutil.createSessionTerminateSuccess(juliet, "thisissparta");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisissparta'>" +
                        "<reason>" +
                        "<success/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, success.getStanzaId(), jingleXML);
        assertXMLEqual(xml, success.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.success);
    }

    @Test
    public void createSessionTerminateBusyTest() throws Exception {
        Jingle busy = jutil.createSessionTerminateBusy(juliet, "thisispatrick");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisispatrick'>" +
                        "<reason>" +
                        "<busy/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, busy.getStanzaId(), jingleXML);
        assertXMLEqual(xml, busy.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.busy);
    }

    @Test
    public void createSessionTerminateAlternativeSessionTest() throws Exception {
        Jingle busy = jutil.createSessionTerminateAlternativeSession(juliet, "thisistherhythm", "ofthenight");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisistherhythm'>" +
                        "<reason>" +
                        "<alternative-session>" +
                        "<sid>ofthenight</sid>" +
                        "</alternative-session>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, busy.getStanzaId(), jingleXML);
        assertXMLEqual(xml, busy.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.alternative_session);
        JingleReason.AlternativeSession alt = (JingleReason.AlternativeSession) jingle.getReason();
        assertEquals("ofthenight", alt.getAlternativeSessionId());
    }

    @Test
    public void createSessionTerminateCancelTest() throws Exception {
        Jingle cancel = jutil.createSessionTerminateCancel(juliet, "thisistheend");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisistheend'>" +
                        "<reason>" +
                        "<cancel/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, cancel.getStanzaId(), jingleXML);
        assertXMLEqual(xml, cancel.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.cancel);
    }

    @Test
    public void createSessionTerminateUnsupportedTransportsTest() throws Exception {
        Jingle unsupportedTransports = jutil.createSessionTerminateUnsupportedTransports(juliet, "thisisus");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisisus'>" +
                        "<reason>" +
                        "<unsupported-transports/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, unsupportedTransports.getStanzaId(), jingleXML);
        assertXMLEqual(xml, unsupportedTransports.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.unsupported_transports);
    }

    @Test
    public void createSessionTerminateUnsupportedApplicationsTest() throws Exception {
        Jingle unsupportedApplications = jutil.createSessionTerminateUnsupportedApplications(juliet, "thisiswar");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisiswar'>" +
                        "<reason>" +
                        "<unsupported-applications/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, unsupportedApplications.getStanzaId(), jingleXML);
        assertXMLEqual(xml, unsupportedApplications.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(jingle.getAction(), JingleAction.session_terminate);
        assertEquals(jingle.getReason().asEnum(), JingleReason.Reason.unsupported_applications);
    }

    @Test
    public void createSessionPingTest() throws Exception {
        Jingle ping = jutil.createSessionPing(juliet, "thisisit");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-info' " +
                        "sid='thisisit'/>";
        String xml = getIQXML(romeo, juliet, ping.getStanzaId(), jingleXML);
        assertXMLEqual(xml, ping.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(JingleAction.session_info, jingle.getAction());
    }

    @Test
    public void createSessionTerminateContentCancelTest() throws Exception {
        Jingle cancel = jutil.createSessionTerminateContentCancel(juliet, "thisismumbo#5", JingleContent.Creator.initiator, "content123");
        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-terminate' " +
                        "sid='thisismumbo#5'>" +
                        "<content creator='initiator' name='content123'/>" +
                        "<reason>" +
                        "<cancel/>" +
                        "</reason>" +
                        "</jingle>";
        String xml = getIQXML(romeo, juliet, cancel.getStanzaId(), jingleXML);
        assertXMLEqual(xml, cancel.toXML().toString());
        Jingle jingle = new JingleProvider().parse(TestUtils.getParser(jingleXML));
        assertNotNull(jingle);
        assertEquals(JingleAction.session_terminate, jingle.getAction());
        assertEquals(JingleReason.Reason.cancel, jingle.getReason().asEnum());
        assertEquals("thisismumbo#5", jingle.getSid());
        JingleContent content = jingle.getContents().get(0);
        assertNotNull(content);
        assertEquals("content123", content.getName());
        assertEquals(JingleContent.Creator.initiator, content.getCreator());
    }

    @Test
    public void createErrorMalformedRequestTest() throws Exception {
        Jingle j = defaultJingle(romeo, "error123");
        IQ error = jutil.createErrorMalformedRequest(j);
        String xml =
                "<iq " +
                        "to='" + romeo + "' " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<bad-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML().toString());
    }

    @Test
    public void createErrorTieBreakTest() throws IOException, SAXException {
        Jingle j = defaultJingle(romeo, "thisistie");
        IQ error = jutil.createErrorTieBreak(j);
        String xml =
                "<iq " +
                        "to='" + romeo + "' " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<conflict xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<tie-break xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML().toString());
    }

    @Test
    public void createErrorUnknownSessionTest() throws IOException, SAXException {
        Jingle j = defaultJingle(romeo, "youknownothingjohnsnow");
        IQ error = jutil.createErrorUnknownSession(j);
        String xml =
                "<iq " +
                        "to='" + romeo + "' " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<item-not-found xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "<unknown-session xmlns='urn:xmpp:jingle:errors:1'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML().toString());
    }

    @Test
    public void createErrorUnknownInitiatorTest() throws IOException, SAXException {
        Jingle j = defaultJingle(romeo, "iamyourfather");
        IQ error = jutil.createErrorUnknownInitiator(j);
        String xml =
                "<iq " +
                        "to='" + romeo + "' " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML().toString());
    }

    @Test
    public void createErrorOutOfOrderTest() throws IOException, SAXException {
        Jingle j = defaultJingle(romeo, "yourfatheriam");
        IQ error = jutil.createErrorOutOfOrder(j);
        String xml =
                "<iq " +
                        "to='" + romeo + "' " +
                        "from='" + romeo + "' " +
                        "id='" + j.getStanzaId() + "' " +
                        "type='error'>" +
                        "<error type='cancel'>" +
                        "<unexpected-result xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" +
                        "</iq>";
        assertXMLEqual(xml, error.toXML().toString());
    }

    private String getIQXML(FullJid from, FullJid to, String stanzaId, String jingleXML) {
        return "<iq from='" + from + "' id='" + stanzaId + "' to='" + to + "' type='set'>" +
                jingleXML +
                "</iq>";
    }

    private Jingle defaultJingle(FullJid recipient, String sessionId) {
        return jutil.createSessionPing(recipient, sessionId);
    }
}
