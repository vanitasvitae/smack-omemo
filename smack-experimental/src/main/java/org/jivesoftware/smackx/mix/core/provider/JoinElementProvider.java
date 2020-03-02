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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.JoinElement;
import org.jivesoftware.smackx.mix.core.element.NickElement;
import org.jivesoftware.smackx.mix.core.element.SubscribeElement;

public abstract class JoinElementProvider<E extends JoinElement> extends ExtensionElementProvider<E> {

    public static class V1 extends JoinElementProvider<JoinElement.V1> {

        @Override
        public JoinElement.V1 parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
            String id = parser.getAttributeValue("", JoinElement.ATTR_ID);

            NickElement nickElement = null;
            List<SubscribeElement> subscribeElements = new ArrayList<>();
            while (true) {
                XmlPullParser.TagEvent tag = parser.nextTag();
                String elementName = parser.getName();
                if (tag == XmlPullParser.TagEvent.START_ELEMENT) {
                    switch (elementName) {
                        case SubscribeElement.ELEMENT:
                            String node = parser.getAttributeValue("", SubscribeElement.ATTR_NODE);
                            subscribeElements.add(new SubscribeElement(node));
                            break;

                        case NickElement.ELEMENT:
                            String nick = parser.nextText();
                            nickElement = new NickElement(nick);
                            break;
                    }
                } else if (JoinElement.ELEMENT.equals(elementName)) {
                    return new JoinElement.V1(id, subscribeElements, nickElement);
                }
            }
        }
    }
}
