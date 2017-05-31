/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.provider;

import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.hash.provider.HashElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadType;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_filetransfer.element.FileTransferPayload;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleContentDescriptionFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.Range;
import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Date;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Provider for JingleContentDescriptionFileTransfer elements.
 */
public class JingleContentDescriptionFileTransferProvider
        extends JingleContentDescriptionProvider<JingleContentDescriptionFileTransfer> {
    @Override
    public JingleContentDescriptionFileTransfer parse(XmlPullParser parser, int initialDepth) throws Exception {

        boolean inRange = false;

        Date date = null;
        String desc = null;
        String mediaType = null;
        String name = null;
        int size = -1;
        Range range = null;
        HashElement inRangeHash = null;
        HashElement hash = null;

        ArrayList<JingleContentDescriptionPayloadType> payloads = new ArrayList<>();

        int offset = 0;
        int length = -1;

        while (true) {

            int tag = parser.nextTag();
            String elem = parser.getName();

            if (tag == START_TAG) {
                switch (elem) {

                    case FileTransferPayload.ELEMENT:
                        date = null;
                        desc = null;
                        mediaType = null;
                        name = null;
                        size = -1;
                        range = null;
                        inRangeHash = null;
                        hash = null;
                        break;

                    case FileTransferPayload.ELEM_DATE:
                        date = XmppDateTime.parseXEP0082Date(parser.nextText());
                        break;

                    case FileTransferPayload.ELEM_DESC:
                        desc = parser.nextText();
                        break;

                    case FileTransferPayload.ELEM_MEDIA_TYPE:
                        mediaType = parser.nextText();
                        break;

                    case FileTransferPayload.ELEM_NAME:
                        name = parser.nextText();
                        break;

                    case FileTransferPayload.ELEM_SIZE:
                        size = Integer.parseInt(parser.nextText());
                        break;

                    case Range.ELEMENT:
                        inRange = true;
                        String offsetString = parser.getAttributeValue(null, Range.ATTR_OFFSET);
                        String lengthString = parser.getAttributeValue(null, Range.ATTR_LENGTH);
                        offset = (offsetString != null ? Integer.parseInt(offsetString) : 0);
                        length = (lengthString != null ? Integer.parseInt(lengthString) : -1);

                        if (parser.isEmptyElementTag()) {
                            range = new Range(offset, length);
                            inRange = false;
                        }
                        break;

                    case HashElement.ELEMENT:
                        if (inRange) {
                            inRangeHash = new HashElementProvider().parse(parser);
                        } else {
                            hash = new HashElementProvider().parse(parser);
                        }
                        break;
                }

            } else if (tag == END_TAG) {
                switch (elem) {

                    case Range.ELEMENT:
                        inRange = false;
                        range = new Range(offset, length, inRangeHash);
                        break;

                    case FileTransferPayload.ELEMENT:
                        payloads.add(new FileTransferPayload(date, desc, hash, mediaType, name, size, range));
                        break;

                    case JingleContentDescriptionFileTransfer.ELEMENT:
                        return new JingleContentDescriptionFileTransfer(payloads);
                }
            }
        }
    }
}
