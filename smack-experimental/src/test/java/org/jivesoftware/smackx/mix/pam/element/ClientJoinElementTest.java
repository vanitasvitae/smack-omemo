package org.jivesoftware.smackx.mix.pam.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.util.Arrays;

import org.jivesoftware.smackx.mix.core.MixNodes;
import org.jivesoftware.smackx.mix.core.element.JoinElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class ClientJoinElementTest {

    @Test
    public void v2Test() {
        String expectedXml = "" +
                "<client-join xmlns='urn:xmpp:mix:pam:2' channel='coven@mix.shakespeare.example'>\n" +
                "  <join xmlns='urn:xmpp:mix:core:1'>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:participants'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:info'/>\n" +
                "  </join>\n" +
                "</client-join>";

        EntityBareJid channel = JidCreate.entityBareFromOrThrowUnchecked("coven@mix.shakespeare.example");
        JoinElement.V1 join = new JoinElement.V1(Arrays.asList(MixNodes.NODE_MESSAGES, MixNodes.NODE_PRESENCE, MixNodes.NODE_PARTICIPANTS, MixNodes.NODE_INFO));
        ClientJoinElement element = new ClientJoinElement.V2(channel, join);

        assertXmlSimilar(expectedXml, element.toXML());
    }
}
