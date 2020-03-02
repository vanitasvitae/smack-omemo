/**
 *
 * Copyright 2020 Paul Schaub
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
