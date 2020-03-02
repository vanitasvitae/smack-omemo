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
