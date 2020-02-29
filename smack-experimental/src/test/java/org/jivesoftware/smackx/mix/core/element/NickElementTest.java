package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class NickElementTest {

    @Test
    public void test() {
        final String expectedXml = "<nick>vanitasvitae</nick>";

        NickElement element = new NickElement("vanitasvitae");

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void testDisallowNullArg() {
        assertThrows(IllegalArgumentException.class, () -> new NickElement(null));
    }

    @Test
    public void testDisallowEmptyArg() {
        assertThrows(IllegalArgumentException.class, () -> new NickElement(" "));
    }
}
