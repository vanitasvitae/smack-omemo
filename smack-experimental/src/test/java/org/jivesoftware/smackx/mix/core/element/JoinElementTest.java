package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smackx.mix.core.MixNodes;

import org.junit.jupiter.api.Test;

public class JoinElementTest {

    @Test
    public void v1ElementTest() {
        final String expectedXml = "" +
                "<join xmlns='urn:xmpp:mix:core:1'\n" +
                "      id='123456'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:participants'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:info'/>\n" +
                "  <nick>third witch</nick>\n" +
                "</join>";
        final String id = "123456";
        final List<SubscribeElement> nodeSubscriptions = Arrays.asList(
                MixNodes.NODE_MESSAGES,
                MixNodes.NODE_PRESENCE,
                MixNodes.NODE_PARTICIPANTS,
                MixNodes.NODE_INFO);
        final String nick = "third witch";

         NickElement nickElement = new NickElement(nick);
        JoinElement joinElement = new JoinElement.V1(id, nodeSubscriptions, nickElement);

        assertXmlSimilar(expectedXml, joinElement.toXML());
        assertEquals(id, joinElement.getId());
        assertEquals(nick, joinElement.getNick().getValue());
        assertEquals(nodeSubscriptions, joinElement.getNodeSubscriptions());
    }
}
