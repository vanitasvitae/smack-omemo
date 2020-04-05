package org.jivesoftware.smackx.mix.misc.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.NickElement;
import org.jivesoftware.smackx.mix.core.element.SetNickElement;
import org.jivesoftware.smackx.mix.misc.element.RegisterElement;

public abstract class RegisterElementProvider<E extends RegisterElement> extends ExtensionElementProvider<E> {

    public static class V0 extends RegisterElementProvider<RegisterElement.V0> {

        @Override
        public RegisterElement.V0 parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
            NickElement nickElement = null;
            while (true) {
                XmlPullParser.TagEvent tagEvent = parser.nextTag();
                String name = parser.getName();
                if (tagEvent == XmlPullParser.TagEvent.START_ELEMENT) {
                    if (NickElement.ELEMENT.equals(name)) {
                        nickElement = new NickElement(parser.nextText());
                    }
                } else if (tagEvent == XmlPullParser.TagEvent.END_ELEMENT) {
                    if (RegisterElement.ELEMENT.equals(name)) {
                        return new RegisterElement.V0(nickElement);
                    }
                }
            }
        }
    }
}
