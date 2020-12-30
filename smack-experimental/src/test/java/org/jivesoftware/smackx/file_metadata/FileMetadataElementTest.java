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
package org.jivesoftware.smackx.file_metadata;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;
import org.jivesoftware.smackx.file_metadata.element.child.DescElement;
import org.jivesoftware.smackx.file_metadata.element.child.DimensionsElement;
import org.jivesoftware.smackx.file_metadata.element.child.LengthElement;
import org.jivesoftware.smackx.file_metadata.element.child.MediaTypeElement;
import org.jivesoftware.smackx.file_metadata.element.child.NameElement;
import org.jivesoftware.smackx.file_metadata.element.child.SizeElement;
import org.jivesoftware.smackx.file_metadata.provider.FileMetadataElementProvider;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;

import org.junit.jupiter.api.Test;
import org.jxmpp.util.XmppDateTime;

public class FileMetadataElementTest extends SmackTestSuite {

    @Test
    public void testSerialization() throws ParseException, XmlPullParserException, IOException, SmackParsingException {
        Date date = XmppDateTime.parseDate("2015-07-26T21:46:00+01:00");
        FileMetadataElement metadataElement = FileMetadataElement.builder()
                .setModificationDate(date)
                .setDimensions("1920x1080")
                .addDescription("Picture of 24th XSF Summit")
                .addDescription("Foto vom 24. XSF Summit", "de")
                .addHash(new HashElement(HashManager.ALGORITHM.SHA_256, "2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU="))
                .setLength(63000)
                .setMediaType("text/plain")
                .setName("text.txt")
                .setSize(6144)
                .build();

        // Test serialization
        final String expectedXml = "<file xmlns='urn:xmpp:file:metadata:0'>" +
                "<date>2015-07-26T20:46:00.000+00:00</date>" +
                "<dimensions>1920x1080</dimensions>" +
                "<desc>Picture of 24th XSF Summit</desc>" +
                "<desc xml:lang='de'>Foto vom 24. XSF Summit</desc>" +
                "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>2XarmwTlNxDAMkvymloX3S5+VbylNrJt/l5QyPa+YoU=</hash>" +
                "<length>63000</length>" +
                "<media-type>text/plain</media-type>" +
                "<name>text.txt</name>" +
                "<size>6144</size>" +
                "</file>";
        assertXmlSimilar(expectedXml, metadataElement.toXML().toString());

        // Test deserialization
        FileMetadataElement parsed = FileMetadataElementProvider.TEST_INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(metadataElement, parsed);
    }

    @Test
    public void nameIsEscaped() {
        NameElement nameElement = new NameElement("/etc/passwd");
        assertEquals("%2Fetc%2Fpasswd", nameElement.getName());
    }

    @Test
    public void rejectNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> new SizeElement(-1));
    }

    @Test
    public void rejectNegativeLength() {
        assertThrows(IllegalArgumentException.class, () -> new LengthElement(-1));
    }

    @Test
    public void rejectEmptyDescription() {
        assertThrows(IllegalArgumentException.class, () -> new DescElement(""));
        assertThrows(IllegalArgumentException.class, () -> new DescElement(null));
    }

    @Test
    public void rejectEmptyDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new DimensionsElement(""));
        assertThrows(IllegalArgumentException.class, () -> new DimensionsElement(null));
    }

    @Test
    public void rejectEmptyNameElement() {
        assertThrows(IllegalArgumentException.class, () -> new NameElement(""));
        assertThrows(IllegalArgumentException.class, () -> new NameElement(null));
    }

    @Test
    public void rejectEmptyMediaTypeElement() {
        assertThrows(IllegalArgumentException.class, () -> new MediaTypeElement(""));
        assertThrows(IllegalArgumentException.class, () -> new MediaTypeElement(null));
    }

    @Test
    public void getDescTest() {
        FileMetadataElement metadataElement = FileMetadataElement.builder()
                .addDescription("Foo", "br")
                .addDescription("Baz")
                .addDescription("Bag", "en")
                .build();

        assertEquals("Foo", metadataElement.getDescElement("br").getDescription());
        assertEquals("Baz", metadataElement.getDescElement(null).getDescription());
        assertEquals("Baz", metadataElement.getDescElement().getDescription());
        assertEquals("Bag", metadataElement.getDescElement("en").getDescription());
        assertNull(metadataElement.getDescElement("null"));
        assertEquals(3, metadataElement.getDescElements().size());
    }
}
