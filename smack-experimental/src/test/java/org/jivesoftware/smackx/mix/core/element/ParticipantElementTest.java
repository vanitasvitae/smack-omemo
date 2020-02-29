package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class ParticipantElementTest {

    @Test
    public void testParticipantElement() {
        final String nick = "thirdwitch";
        final EntityBareJid jid = JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example");
        final String expectedXml = "" +
                "<participant xmlns='urn:xmpp:mix:core:1'>\n" +
                "  <nick>thirdwitch</nick>\n" +
                "  <jid>hag66@shakespeare.example</jid>\n" +
                "</participant>";

        ParticipantElement participantElement = new ParticipantElement.V1(nick, jid);

        assertXmlSimilar(expectedXml, participantElement.toXML());
        assertEquals(nick, participantElement.getNick().getValue());
        assertEquals(jid, participantElement.getJid().getValue());
    }
}
