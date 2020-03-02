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
import org.jivesoftware.smackx.mix.core.MixNodes;
import org.jivesoftware.smackx.mix.core.element.UpdateSubscriptionElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class UpdateSubscriptionElementProviderTest {

    @Test
    public void v1ProviderWithoutJidTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
                "<update-subscription xmlns='urn:xmpp:mix:core:1'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "</update-subscription>";
        UpdateSubscriptionElementProvider.V1 provider = new UpdateSubscriptionElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        UpdateSubscriptionElement.V1 element = provider.parse(parser);

        assertNotNull(element);
        assertEquals(1, element.getNodeSubscriptions().size());
        assertEquals(MixNodes.NODE_MESSAGES, element.getNodeSubscriptions().get(0));
    }

    @Test
    public void v1ProviderWithJidTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<update-subscription xmlns='urn:xmpp:mix:core:1'\n" +
                "                     jid='hag66@shakespeare.example'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
                "</update-subscription>";
        EntityBareJid jid = JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example");
        UpdateSubscriptionElementProvider.V1 provider = new UpdateSubscriptionElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        UpdateSubscriptionElement.V1 element = provider.parse(parser);

        assertNotNull(element);
        assertEquals(2, element.getNodeSubscriptions().size());
        assertEquals(jid, element.getJid());
    }
}
