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
import org.jivesoftware.smackx.mix.core.element.JoinElement;

import org.junit.jupiter.api.Test;

public class JoinElementProviderTest {

    @Test
    public void v1ProviderTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
            "<join xmlns='urn:xmpp:mix:core:1'\n" +
            "      id='123456'>\n" +
            "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
            "  <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
            "  <subscribe node='urn:xmpp:mix:nodes:participants'/>\n" +
            "  <subscribe node='urn:xmpp:mix:nodes:info'/>\n" +
            "  <nick>third witch</nick>\n" +
            "</join>";
        JoinElementProvider.V1 provider = new JoinElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        JoinElement.V1 element = provider.parse(parser);

        assertEquals("123456", element.getId());

        assertNotNull(element.getNick());
        assertEquals("third witch", element.getNick().getValue());

        assertEquals(4, element.getNodeSubscriptions().size());
        assertEquals(MixNodes.NODE_MESSAGES, element.getNodeSubscriptions().get(0));
        assertEquals(MixNodes.NODE_PRESENCE, element.getNodeSubscriptions().get(1));
        assertEquals(MixNodes.NODE_PARTICIPANTS, element.getNodeSubscriptions().get(2));
        assertEquals(MixNodes.NODE_INFO, element.getNodeSubscriptions().get(3));
    }

    @Test
    public void ignoreUnknownElementsTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
                "<join xmlns='urn:xmpp:mix:core:1'\n" +
                "      id='123456'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "  <ignore me='please'/>\n" +
                "  <nick>third witch</nick>\n" +
                "</join>";
        JoinElementProvider.V1 provider = new JoinElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        JoinElement.V1 element = provider.parse(parser);
        assertEquals("123456", element.getId());
        assertEquals(1, element.getNodeSubscriptions().size());
        assertEquals("third witch", element.getNick().getValue());
    }

    @Test
    public void ignoreUnknownAttributeTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "" +
                "<join xmlns='urn:xmpp:mix:core:1'\n" +
                "      id='123456' ignore='me'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "  <nick>third witch</nick>\n" +
                "</join>";
        JoinElementProvider.V1 provider = new JoinElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        JoinElement.V1 element = provider.parse(parser);
        assertEquals("123456", element.getId());
        assertEquals(1, element.getNodeSubscriptions().size());
        assertEquals("third witch", element.getNick().getValue());
    }
}
