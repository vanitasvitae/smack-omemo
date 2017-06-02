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
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.hash.HashManager;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferPayloadElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleContentDescriptionFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleContentDescriptionFileTransferProvider;
import org.junit.Test;
import org.jxmpp.util.XmppDateTime;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the JingleContentDescriptionFileTransfer element and provider.
 */
public class JingleContentDescriptionFileTransferTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        String dateString = "2012-01-02T03:04:05.000+00:00";
        String descriptionString = "The Description";
        String mediaTypeString = "text/plain";
        String nameString = "the-file.txt";
        int sizeInt = 4096;
        HashManager.ALGORITHM algorithm = HashManager.ALGORITHM.SHA_256;
        String hashB64 = "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=";
        String xml =
                "<description xmlns='urn:xmpp:jingle:apps:file-transfer:5'>" +
                    "<file>" +
                        "<date>" + dateString + "</date>" +
                        "<desc>" + descriptionString + "</desc>" +
                        "<media-type>" + mediaTypeString + "</media-type>" +
                        "<name>" + nameString + "</name>" +
                        "<range/>" +
                        "<size>" + sizeInt + "</size>" +
                        "<hash xmlns='urn:xmpp:hashes:2' algo='" + algorithm + "'>" +
                            hashB64 +
                        "</hash>" +
                    "</file>" +
                "</description>";
        HashElement hashElement = new HashElement(algorithm, hashB64);
        Range range = new Range();
        Date date = XmppDateTime.parseDate(dateString);
        JingleFileTransferPayloadElement jingleFileTransferPayloadElement = new JingleFileTransferPayloadElement(date, descriptionString, hashElement, mediaTypeString, nameString, sizeInt, range);
        ArrayList<JingleContentDescriptionPayloadElement> payloads = new ArrayList<>();
        payloads.add(jingleFileTransferPayloadElement);

        JingleContentDescriptionFileTransfer descriptionFileTransfer =
                new JingleContentDescriptionFileTransfer(payloads);
        assertEquals(xml, descriptionFileTransfer.toXML().toString());

        JingleContentDescription parsed = new JingleContentDescriptionFileTransferProvider()
                .parse(TestUtils.getParser(xml));
        assertEquals(xml, parsed.toXML().toString());

        JingleFileTransferPayloadElement payload = (JingleFileTransferPayloadElement) parsed.getJinglePayloadTypes().get(0);
        assertEquals(date, payload.getDate());
        assertEquals(descriptionString, payload.getDescription());
        assertEquals(mediaTypeString, payload.getMediaType());
        assertEquals(nameString, payload.getName());
        assertEquals(sizeInt, payload.getSize());
        assertEquals(range, payload.getRange());
        assertEquals(hashElement, payload.getHash());

        JingleContentDescriptionFileTransfer descriptionFileTransfer1 = new JingleContentDescriptionFileTransfer(null);
        assertNotNull(descriptionFileTransfer1.getJinglePayloadTypes());
    }

    @Test
    public void parserTest2() throws Exception {
        HashManager.ALGORITHM algorithm = HashManager.ALGORITHM.SHA_256;
        String hashB64 = "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=";
        HashElement hashElement = new HashElement(algorithm, hashB64);
        Range range = new Range(2048, 1024, hashElement);
        String xml =
                "<description xmlns='urn:xmpp:jingle:apps:file-transfer:5'>" +
                    "<file>" +
                        "<range offset='2048' length='1024'>" +
                            "<hash xmlns='urn:xmpp:hashes:2' algo='" + algorithm + "'>" +
                                hashB64 +
                            "</hash>" +
                        "</range>" +
                    "</file>" +
                "</description>";
        JingleFileTransferPayloadElement payload = new JingleFileTransferPayloadElement(null, null, null, null, null, -1, range);
        ArrayList<JingleContentDescriptionPayloadElement> list = new ArrayList<>();
        list.add(payload);
        JingleContentDescriptionFileTransfer fileTransfer = new JingleContentDescriptionFileTransfer(list);
        assertEquals(xml, fileTransfer.toXML().toString());
        JingleContentDescriptionFileTransfer parsed = new JingleContentDescriptionFileTransferProvider()
                .parse(TestUtils.getParser(xml));
        assertEquals(xml, parsed.toXML().toString());
    }
}
