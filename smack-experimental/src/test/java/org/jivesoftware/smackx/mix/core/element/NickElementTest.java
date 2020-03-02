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
