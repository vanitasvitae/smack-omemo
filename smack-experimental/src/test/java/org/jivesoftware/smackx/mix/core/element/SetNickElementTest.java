package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SetNickElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "" +
                "<setnick xmlns='urn:xmpp:mix:core:1'>\n" +
                "    <nick>thirdwitch</nick>\n" +
                "</setnick>";

        SetNickElement element = new SetNickElement.V1("thirdwitch");

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void disallowNullNickTest() {
        assertThrows(IllegalArgumentException.class, () -> new SetNickElement.V1((NickElement) null));
    }
}
