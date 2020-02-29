package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class CreateChannelElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "<create channel='coven' xmlns='urn:xmpp:mix:core:1'/>";

        CreateChannelElement element = new CreateChannelElement.V1("coven");

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void v1DisallowEmptyChannelNameTest() {
        assertThrows(IllegalArgumentException.class, () -> new CreateChannelElement.V1(" "));
    }

    @Test
    public void v1AllowNullChannelNameTest() {
        assertDoesNotThrow(() -> new CreateChannelElement.V1());
        assertDoesNotThrow(() -> new CreateChannelElement.V1(null));
    }
}
