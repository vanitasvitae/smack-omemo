package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class JidElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "<jid>hag66@shakespeare.example</jid>";

        EntityBareJid jid = JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example");
        JidElement element = new JidElement(jid);

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void v1DisallowNullArg() {
        assertThrows(IllegalArgumentException.class, () -> new JidElement(null));
    }
}
