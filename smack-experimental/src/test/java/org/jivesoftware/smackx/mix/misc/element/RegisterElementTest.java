package org.jivesoftware.smackx.mix.misc.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.jivesoftware.smackx.mix.core.element.NickElement;

import org.junit.jupiter.api.Test;

public class RegisterElementTest {

    @Test
    public void testV0Serialization() {
        RegisterElement register = new RegisterElement.V0(new NickElement("thirdwitch"));
        String expectedXml = "" +
                "<register xmlns='urn:xmpp:mix:misc:0'>" +
                "  <nick>thirdwitch</nick>" +
                "</register>";

        assertXmlSimilar(expectedXml, register.toXML());
    }

    @Test
    public void testV0NoNickSerialization() {
        RegisterElement register = new RegisterElement.V0();
        String expectedXml = "" +
                "<register xmlns='urn:xmpp:mix:misc:0'>" +
                "</register>";

        assertXmlSimilar(expectedXml, register.toXML());
    }
}
