/**
 *
 * Copyright 2020 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
