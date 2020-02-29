package org.jivesoftware.smackx.mix.core.element;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.util.Collections;

import org.jivesoftware.smackx.mix.core.MixNodes;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class UpdateSubscriptionElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "" +
                "<update-subscription xmlns='urn:xmpp:mix:core:1'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "</update-subscription>";

        UpdateSubscriptionElement element = new UpdateSubscriptionElement.V1(
                Collections.singletonList(MixNodes.NODE_MESSAGES));

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void v1TestWithJid() {
        String expectedXml = "" +
                "<update-subscription xmlns='urn:xmpp:mix:core:1'\n" +
                "                     jid='hag66@shakespeare.example'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "</update-subscription>";

        UpdateSubscriptionElement element = new UpdateSubscriptionElement.V1(
                Collections.singletonList(MixNodes.NODE_MESSAGES),
                JidCreate.entityBareFromOrThrowUnchecked("hag66@shakespeare.example"));

        assertXmlSimilar(expectedXml, element.toXML());
    }
}
