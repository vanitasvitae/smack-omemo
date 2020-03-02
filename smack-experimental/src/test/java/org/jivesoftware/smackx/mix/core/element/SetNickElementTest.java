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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SetNickElementTest {

    @Test
    public void v1Test() {
        String expectedXml = "" +
                "<setnick xmlns='urn:xmpp:mix:core:1'>\n" +
                "    <nick>thirdwitch</nick>\n" +
                "</setnick>";

        SetNickElement element = new SetNickElement.V1("thirdwitch");

        assertXmlSimilar(expectedXml, element.toXML());
    }

    @Test
    public void disallowNullNickTest() {
        assertThrows(IllegalArgumentException.class, () -> new SetNickElement.V1((NickElement) null));
    }
}
