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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.CreateChannelElement;

import org.junit.jupiter.api.Test;

public class CreateChannelElementProviderTest {

    @Test
    public void v1ProviderEmptyElementTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<create xmlns='urn:xmpp:mix:core:1'/>";
        CreateChannelElementProvider.V1 provider = new CreateChannelElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        CreateChannelElement.V1 element = provider.parse(parser);
        assertNull(element.getChannel());
    }

    @Test
    public void v1ProviderWithChannelTest() throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<create channel='coven' xmlns='urn:xmpp:mix:core:1'/>";
        String channel = "coven";
        CreateChannelElementProvider.V1 provider = new CreateChannelElementProvider.V1();
        XmlPullParser parser = TestUtils.getParser(xml);

        CreateChannelElement.V1 element = provider.parse(parser);
        assertEquals(channel, element.getChannel());
    }
}
