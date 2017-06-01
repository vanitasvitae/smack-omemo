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

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_filetransfer.element.Checksum;
import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for the Checksum element.
 */
public class ChecksumProvider extends ExtensionElementProvider<Checksum> {
    @Override
    public Checksum parse(XmlPullParser parser, int initialDepth) throws Exception {
        JingleContent.Creator creator = null;
        String creatorString = parser.getAttributeValue(null, Checksum.ATTR_CREATOR);
        if (creatorString != null) {
            creator = JingleContent.Creator.valueOf(creatorString);
        }
        String name = parser.getAttributeValue(null, Checksum.ATTR_NAME);
        //TODO JingleFileTransferPayload file = new JingleFileTransferPayloadProvider().parse(parser);

        return new Checksum(creator, name, null);
    }
}
