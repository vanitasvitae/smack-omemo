/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hash;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.hash.provider.HashElementProvider;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.jivesoftware.smackx.hash.HashManager.ALGORITHM.SHA_256;
import static org.junit.Assert.assertArrayEquals;

/**
 * Test toXML and parse of HashElement and HashElementProvider.
 */
public class HashElementTest extends SmackTestSuite {

    @Test
    public void stanzaTest() throws Exception {
        String message = "Hello World!";
        HashElement element = HashElement.fromData(SHA_256, message.getBytes(StringUtils.UTF8));
        String expected = "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>";
        assertEquals(expected, element.toXML().toString());

        HashElement parsed = new HashElementProvider().parse(TestUtils.getParser(expected));
        assertEquals(expected, parsed.toXML().toString());
        assertEquals(SHA_256, parsed.getAlgorithm());
        assertEquals("f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=", parsed.getHashB64());
        assertArrayEquals(HashManager.sha_256(message.getBytes(StringUtils.UTF8)), parsed.getHash());
    }

}
