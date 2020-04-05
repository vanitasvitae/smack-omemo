package org.jivesoftware.smackx.mix.misc.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.mix.misc.element.RegisterElement;

import org.junit.jupiter.api.Test;

public class RegisterElementProviderTest {

    private final RegisterElementProvider.V0 provider = new RegisterElementProvider.V0();

    @Test
    public void testV0Parsing() throws XmlPullParserException, IOException, SmackParsingException {
        final String xml = "" +
                "<register xmlns='urn:xmpp:mix:misc:0'>" +
                "  <nick>thirdwitch</nick>" +
                "</register>";
        final String nick = "thirdwitch";
        XmlPullParser parser = TestUtils.getParser(xml);

        RegisterElement parsed = provider.parse(parser);
        assertEquals(nick, parsed.getNick().getValue());
    }

    @Test
    public void testV0NoNickElementParsing() throws XmlPullParserException, IOException, SmackParsingException {
        final String noChildElementXml = "<register xmlns='urn:xmpp:mix:misc:0'></register>";
        XmlPullParser noChildElementParser = TestUtils.getParser(noChildElementXml);

        RegisterElement noChildElementParsed = provider.parse(noChildElementParser);

        assertNull(noChildElementParsed.getNick());
    }

    @Test
    public void testV0EmptyParsing() throws XmlPullParserException, IOException, SmackParsingException {
        final String emptyElementXml = "<register xmlns='urn:xmpp:mix:misc:0'/>";
        XmlPullParser emptyElementParser = TestUtils.getParser(emptyElementXml);

        RegisterElement emptyElementParsed = provider.parse(emptyElementParser);

        assertNull(emptyElementParsed.getNick());
    }


}
