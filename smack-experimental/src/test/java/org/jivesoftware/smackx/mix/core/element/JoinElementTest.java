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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smackx.mix.core.MixNodes;

import org.junit.jupiter.api.Test;

public class JoinElementTest {

    @Test
    public void v1ElementTest() {
        final String expectedXml = "" +
                "<join xmlns='urn:xmpp:mix:core:1'\n" +
                "      id='123456'>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:messages'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:presence'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:participants'/>\n" +
                "  <subscribe node='urn:xmpp:mix:nodes:info'/>\n" +
                "  <nick>third witch</nick>\n" +
                "</join>";
        final String id = "123456";
        final List<SubscribeElement> nodeSubscriptions = Arrays.asList(
                MixNodes.NODE_MESSAGES,
                MixNodes.NODE_PRESENCE,
                MixNodes.NODE_PARTICIPANTS,
                MixNodes.NODE_INFO);
        final String nick = "third witch";

         NickElement nickElement = new NickElement(nick);
        JoinElement joinElement = new JoinElement.V1(id, nodeSubscriptions, nickElement);

        assertXmlSimilar(expectedXml, joinElement.toXML());
        assertEquals(id, joinElement.getId());
        assertEquals(nick, joinElement.getNick().getValue());
        assertEquals(nodeSubscriptions, joinElement.getNodeSubscriptions());
    }
}
