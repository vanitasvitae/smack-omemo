package org.jivesoftware.smackx.mix.core.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.core.element.LeaveElement;

public abstract class LeaveElementProvider extends ExtensionElementProvider<LeaveElement> {

    public static class V1 extends LeaveElementProvider {

        @Override
        public LeaveElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                throws XmlPullParserException, IOException, SmackParsingException {
            return new LeaveElement.V1();
        }
    }
}
