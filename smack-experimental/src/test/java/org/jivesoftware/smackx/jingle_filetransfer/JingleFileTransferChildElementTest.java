/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

/**
 * Test the JingleContentFile class.
 */
public class JingleFileTransferChildElementTest extends SmackTestSuite {

    @Test
    public void rangeTest() throws Exception {
        Range range = new Range();
        String xml = "<range/>";
        assertEquals(0, range.getOffset());
        assertEquals(-1, range.getLength());
        assertNull(range.getHash());
        assertEquals(xml, range.toXML().toString());

        range = new Range(4096);
        xml = "<range length='4096'/>";
        assertEquals(4096, range.getLength());
        assertEquals(0, range.getOffset());
        assertNull(range.getHash());
        assertEquals(xml, range.toXML().toString());

        range = new Range(256, 1024);
        xml = "<range offset='256' length='1024'/>";
        assertEquals(256, range.getOffset());
        assertEquals(1024, range.getLength());
        assertNull(range.getHash());
        assertEquals(xml, range.toXML().toString());

        String hashB64 = "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=";
        HashElement hashElement = new HashElement(HashManager.ALGORITHM.SHA_256, hashB64);
        range = new Range(0, 35, hashElement);
        xml =   "<range length='35'>" +
                "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>" +
                "</range>";
        assertEquals(0, range.getOffset());
        assertEquals(35, range.getLength());
        assertNotNull(range.getHash());
        assertEquals(range.getHash().toXML().toString(), hashElement.toXML().toString());
        assertEquals(xml, range.toXML().toString());
    }
}
