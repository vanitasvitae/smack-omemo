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

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.JidElement;
import org.jivesoftware.smackx.mix.core.element.NickElement;
import org.jivesoftware.smackx.mix.core.element.ParticipantElement;

import org.jxmpp.jid.impl.JidCreate;

public abstract class ParticipantElementProvider<E extends ParticipantElement> extends ExtensionElementProvider<E> {

    public static class V1 extends ParticipantElementProvider<ParticipantElement.V1> {

        @Override
        public ParticipantElement.V1 parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
            NickElement nickElement = null;
            JidElement jidElement = null;
            while (true) {
                XmlPullParser.TagEvent tag = parser.nextTag();
                String name = parser.getName();
                if (tag == XmlPullParser.TagEvent.START_ELEMENT) {
                    switch (name) {
                        case NickElement.ELEMENT:
                            nickElement = new NickElement(parser.nextText());
                            break;

                        case JidElement.ELEMENT:
                            jidElement = new JidElement(JidCreate.entityBareFrom(parser.nextText()));
                            break;
                    }
                } else if (ParticipantElement.ELEMENT.equals(name)) {
                    return new ParticipantElement.V1(nickElement, jidElement);
                }
            }
        }
    }
}
