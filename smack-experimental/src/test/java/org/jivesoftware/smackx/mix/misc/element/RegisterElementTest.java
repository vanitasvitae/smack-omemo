package org.jivesoftware.smackx.mix.misc.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.jivesoftware.smackx.mix.core.element.NickElement;

import org.junit.jupiter.api.Test;

public class RegisterElementTest {

    @Test
    public void v0testSerialization() {
        RegisterElement register = new RegisterElement.V0(new NickElement("thirdwitch"));
        String expectedXml = "" +
                "<register xmlns='urn:xmpp:mix:misc:0'>\n" +
                "  <nick>thirdwitch</nick>\n" +
                "</register>\n";

        assertXmlSimilar(expectedXml, register.toXML());
    }
}
