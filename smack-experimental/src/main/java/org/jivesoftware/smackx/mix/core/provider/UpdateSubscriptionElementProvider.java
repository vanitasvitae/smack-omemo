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
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.SubscribeElement;
import org.jivesoftware.smackx.mix.core.element.UpdateSubscriptionElement;

import org.jxmpp.jid.EntityBareJid;

public abstract class UpdateSubscriptionElementProvider<E extends UpdateSubscriptionElement> extends ExtensionElementProvider<E> {

    public static class V1 extends UpdateSubscriptionElementProvider<UpdateSubscriptionElement.V1> {

        @Override
        public UpdateSubscriptionElement.V1 parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
            EntityBareJid jid = ParserUtils.getBareJidAttribute(parser, UpdateSubscriptionElement.ATTR_JID);
            List<SubscribeElement> subscribeElementList = new ArrayList<>();

            while(true) {
                XmlPullParser.TagEvent tag = parser.nextTag();
                String name = parser.getName();
                if (tag == XmlPullParser.TagEvent.START_ELEMENT) {
                    if (name.equals(SubscribeElement.ELEMENT)) {
                        subscribeElementList.add(new SubscribeElement(parser.getAttributeValue("", SubscribeElement.ATTR_NODE)));
                    }
                } else if (tag == XmlPullParser.TagEvent.END_ELEMENT) {
                    if (name.equals(UpdateSubscriptionElement.ELEMENT)) {
                        return new UpdateSubscriptionElement.V1(subscribeElementList, jid);
                    }
                }
            }
        }
    }
}
