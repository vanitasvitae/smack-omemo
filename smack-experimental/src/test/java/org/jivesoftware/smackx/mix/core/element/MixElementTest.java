package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class MixElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "" +
                "<mix xmlns='urn:xmpp:mix:core:1'>\n" +
                "  <nick>thirdwitch</nick>\n" +
                "  <jid>hag66@shakespeare.example</jid>\n" +
                "</mix>";

        NickElement nick = new NickElement("thirdwitch");
        JidElement jid = new JidElement(JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example"));
        MixElement element = new MixElement.V1(nick, jid);

        assertXmlSimilar(expectedXml, element.toXML());
    }
}
