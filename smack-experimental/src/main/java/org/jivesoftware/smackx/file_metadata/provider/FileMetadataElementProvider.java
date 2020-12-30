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
package org.jivesoftware.smackx.file_metadata.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;
import org.jivesoftware.smackx.file_metadata.element.child.DateElement;
import org.jivesoftware.smackx.file_metadata.element.child.DescElement;
import org.jivesoftware.smackx.file_metadata.element.child.DimensionsElement;
import org.jivesoftware.smackx.file_metadata.element.child.LengthElement;
import org.jivesoftware.smackx.file_metadata.element.child.MediaTypeElement;
import org.jivesoftware.smackx.file_metadata.element.child.NameElement;
import org.jivesoftware.smackx.file_metadata.element.child.SizeElement;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;

public class FileMetadataElementProvider extends ExtensionElementProvider<FileMetadataElement> {

    public static FileMetadataElementProvider TEST_INSTANCE = new FileMetadataElementProvider();

    @Override
    public FileMetadataElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        FileMetadataElement.Builder builder = FileMetadataElement.builder();
        do {
            XmlPullParser.TagEvent tagEvent = parser.nextTag();
            String name = parser.getName();
            if (tagEvent != XmlPullParser.TagEvent.START_ELEMENT) {
                continue;
            }
            switch (name) {
                case FileMetadataElement.ELEMENT:
                    parser.next();
                    break;
                case DateElement.ELEMENT:
                    builder.setModificationDate(ParserUtils.getDateFromNextText(parser));
                    break;
                case DescElement.ELEMENT:
                    String lang = ParserUtils.getXmlLang(parser);
                    builder.addDescription(ParserUtils.getRequiredNextText(parser), lang);
                    break;
                case DimensionsElement.ELEMENT:
                    builder.setDimensions(ParserUtils.getRequiredNextText(parser));
                    break;
                case LengthElement.ELEMENT:
                    builder.setLength(Long.parseLong(ParserUtils.getRequiredNextText(parser)));
                    break;
                case MediaTypeElement.ELEMENT:
                    builder.setMediaType(ParserUtils.getRequiredNextText(parser));
                    break;
                case NameElement.ELEMENT:
                    builder.setName(ParserUtils.getRequiredNextText(parser));
                    break;
                case SizeElement.ELEMENT:
                    builder.setSize(Long.parseLong(ParserUtils.getRequiredNextText(parser)));
                    break;
                case HashElement.ELEMENT:
                    builder.addHash(HashElementProvider.INSTANCE.parse(parser, parser.getDepth(), xmlEnvironment));
                    break;
            }
        } while (parser.getDepth() != initialDepth);
        return builder.build();
    }
}
