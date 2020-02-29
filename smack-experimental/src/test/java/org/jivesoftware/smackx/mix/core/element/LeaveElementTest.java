package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.junit.jupiter.api.Test;

public class LeaveElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "<leave xmlns='urn:xmpp:mix:core:1'/>";

        LeaveElement element = new LeaveElement.V1();

        assertXmlSimilar(expectedXml, element.toXML());
    }
}
