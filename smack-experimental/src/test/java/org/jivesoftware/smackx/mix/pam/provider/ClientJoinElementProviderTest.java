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
package org.jivesoftware.smackx.mix.pam.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.MixNodes;
import org.jivesoftware.smackx.mix.core.element.SubscribeElement;
import org.jivesoftware.smackx.mix.pam.element.ClientJoinElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class ClientJoinElementProviderTest {

    @Test
    public void v2ProviderTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
                "<client-join xmlns='urn:xmpp:mix:pam:2' channel='coven@mix.shakespeare.example'>\n" +
                "  <join xmlns='urn:xmpp:mix:core:1'>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:participants'/>\n" +
                "    <subscribe node='urn:xmpp:mix:nodes:info'/>\n" +
                "  </join>\n" +
                "</client-join>";
        EntityBareJid channel = JidCreate.entityBareFromOrThrowUnchecked("coven@mix.shakespeare.example");
        List<SubscribeElement> subscribeElements = Arrays.asList(
                MixNodes.NODE_MESSAGES, MixNodes.NODE_PRESENCE, MixNodes.NODE_PARTICIPANTS, MixNodes.NODE_INFO);
        ClientJoinElementProvider.V2 provider = new ClientJoinElementProvider.V2();
        XmlPullParser parser = TestUtils.getParser(xml);

        ClientJoinElement.V2 element = provider.parse(parser);

        assertEquals(channel, element.getChannel());
        assertEquals(4, element.getJoin().getNodeSubscriptions().size());
        assertNull(element.getJoin().getId());
        assertNull(element.getJoin().getNick());
        assertEquals(subscribeElements, element.getJoin().getNodeSubscriptions());
    }
}
