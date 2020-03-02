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
package org.jivesoftware.smackx.mix.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.ParticipantElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class ParticipantElementProviderTest {

    @Test
    public void v1ProviderTest() throws XmlPullParserException, IOException, SmackParsingException {
        final String xml = "" +
                "<participant xmlns='urn:xmpp:mix:core:1'>\n" +
                "  <nick>thirdwitch</nick>\n" +
                "  <jid>hag66@shakespeare.example</jid>\n" +
                "</participant>";
        EntityBareJid jid = JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example");
        String nick = "thirdwitch";
        ParticipantElementProvider.V1 provider = new ParticipantElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        ParticipantElement.V1 element = provider.parse(parser);

        assertNotNull(element);
        assertEquals(jid, element.getJid().getValue());
        assertEquals(nick, element.getNick().getValue());
    }
}
