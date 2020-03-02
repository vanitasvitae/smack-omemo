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

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.JoinElement;
import org.jivesoftware.smackx.mix.core.provider.JoinElementProvider;
import org.jivesoftware.smackx.mix.pam.element.ClientJoinElement;

import org.jxmpp.jid.EntityBareJid;

public abstract class ClientJoinElementProvider<E extends ClientJoinElement> extends ExtensionElementProvider<E> {

    public static class V2 extends ClientJoinElementProvider<ClientJoinElement.V2> {
        JoinElementProvider.V1 joinElementProvider = new JoinElementProvider.V1();

        @Override
        public ClientJoinElement.V2 parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
            EntityBareJid channel = ParserUtils.getBareJidAttribute(parser, ClientJoinElement.ATTR_CHANNEL);
            JoinElement.V1 joinElement = null;

            while (true) {
                XmlPullParser.TagEvent tagEvent = parser.nextTag();
                String name = parser.getName();
                if (tagEvent == XmlPullParser.TagEvent.START_ELEMENT) {
                    if (name.equals(JoinElement.ELEMENT)) {
                        joinElement = joinElementProvider.parse(parser);
                    }
                } else if (tagEvent == XmlPullParser.TagEvent.END_ELEMENT) {
                    if (name.equals(ClientJoinElement.ELEMENT)) {
                        return new ClientJoinElement.V2(channel, joinElement);
                    }
                }
            }
        }
    }
}
