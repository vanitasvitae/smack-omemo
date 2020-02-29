package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DestroyChannelElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "<destroy channel='coven' xmlns='urn:xmpp:mix:core:1'/>";

        DestroyChannelElement element = new DestroyChannelElement.V1("coven");

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void v1DisallowNullChannel() {
        assertThrows(IllegalArgumentException.class, () -> new DestroyChannelElement.V1(null));
    }

    @Test
    public void v1DisallowEmptyChannel() {
        assertThrows(IllegalArgumentException.class, () -> new DestroyChannelElement.V1(" "));
    }
}
