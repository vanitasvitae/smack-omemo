package org.jivesoftware.smackx.jingle_filetransfer;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.JingleUtilTest;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;

import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppDateTime;
import org.xml.sax.SAXException;

/**
 * Created by vanitas on 12.07.17.
 */
public class JingleUtilFileTransferTest extends SmackTestSuite {
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
    public void createSessionInitiateTest() throws IOException, SAXException {
        JingleIBBTransport transport = new JingleIBBTransport("transid");
        Date date = new Date();
        HashElement hash = new HashElement(HashManager.ALGORITHM.SHA_256, "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=");
        JingleFileTransferChild file = new JingleFileTransferChild(date, "desc", hash, "application/octet-string", "name", 1337, null);
        JingleFileTransfer description = new JingleFileTransfer(Collections.<JingleContentDescriptionChildElement>singletonList(file));
        Jingle initiate = jutil.createSessionInitiate(juliet, "letsstart", JingleContent.Creator.initiator, "content", JingleContent.Senders.initiator, description, transport);

        assertEquals(JingleAction.session_initiate, initiate.getAction());
        assertEquals(romeo, initiate.getInitiator());
        assertNull(initiate.getResponder()); //Must be null
        assertEquals("letsstart", initiate.getSid());
        assertEquals(1, initiate.getContents().size());

        JingleContent content = initiate.getContents().get(0);
        assertEquals("content", content.getName());
        assertEquals(JingleContent.Creator.initiator, content.getCreator());
        assertEquals(JingleContent.Senders.initiator, content.getSenders());

        assertEquals(1, description.getJingleContentDescriptionChildren().size());
        assertEquals(file, description.getJingleContentDescriptionChildren().get(0));
        assertEquals(JingleFileTransferChild.ELEMENT, file.getElementName());
        assertEquals(JingleFileTransfer.NAMESPACE_V5, description.getNamespace());
        assertEquals(date, file.getDate());
        assertEquals(hash, file.getHash());
        assertEquals("application/octet-string", file.getMediaType());
        assertEquals("name", file.getName());
        assertEquals(1337, file.getSize());
        assertNull(file.getRange());

        assertEquals(transport, content.getJingleTransport());
        assertEquals("transid", transport.getSessionId());
        assertEquals(JingleIBBTransport.DEFAULT_BLOCK_SIZE, transport.getBlockSize());

        String transportXML =
                "<transport xmlns='urn:xmpp:jingle:transports:ibb:1' " +
                "block-size='4096' " +
                "sid='transid'/>";
        assertXMLEqual(transportXML, transport.toXML().toString());

        String descriptionXML =
                "<description xmlns='urn:xmpp:jingle:apps:file-transfer:5'>" +
                "<file>" +
                "<date>" + XmppDateTime.formatXEP0082Date(date) + "</date>" +
                "<desc>desc</desc>" +
                "<media-type>application/octet-string</media-type>" +
                "<name>name</name>" +
                //"<range/>" + TODO: insert empty element when null?
                "<size>1337</size>" +
                "<hash xmlns='urn:xmpp:hashes:2' " +
                "algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>" +
                "</file>" +
                "</description>";
        assertXMLEqual(descriptionXML, description.toXML().toString());

        String contentXML = "<content creator='initiator' name='content' senders='initiator'>" +
                descriptionXML +
                transportXML +
                "</content>";
        assertXMLEqual(contentXML, content.toXML().toString());

        String jingleXML =
                "<jingle xmlns='urn:xmpp:jingle:1' " +
                        "action='session-initiate' " +
                        "initiator='" + romeo + "' " +
                        "sid='letsstart'>" +
                        contentXML +
                        "</jingle>";
        String xml = JingleUtilTest.getIQXML(romeo, juliet, initiate.getStanzaId(), jingleXML);
        assertXMLEqual(xml, initiate.toXML().toString());
    }
}
