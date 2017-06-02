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

import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleContentDescriptionFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferPayloadElement;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Provider for JingleContentDescriptionFileTransfer elements.
 */
public class JingleContentDescriptionFileTransferProvider
        extends JingleContentDescriptionProvider<JingleContentDescriptionFileTransfer> {
    @Override
    public JingleContentDescriptionFileTransfer parse(XmlPullParser parser, int initialDepth) throws Exception {
        ArrayList<JingleContentDescriptionPayloadElement> payloads = new ArrayList<>();
        while (true) {
            int tag = parser.nextTag();
            String name = parser.getName();

            if (tag == START_TAG) {
                switch (name) {
                    case JingleFileTransferPayloadElement.ELEMENT:
                        payloads.add(new JingleFileTransferPayloadProvider().parse(parser));
                        break;
                }
            }

            if (tag == END_TAG) {
                if (name.equals(JingleContentDescriptionFileTransfer.ELEMENT)) {
                    return new JingleContentDescriptionFileTransfer(payloads);
                }
            }
        }
    }
}
