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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionPayloadType;

import java.util.Date;

/**
 * Content of type File.
 */
public class FileTransferPayload extends JingleContentDescriptionPayloadType {
    public static final String ELEMENT = "file";
    public static final String ELEM_DATE = "date";
    public static final String ELEM_DESC = "desc";
    public static final String ELEM_MEDIA_TYPE = "media-type";
    public static final String ELEM_NAME = "name";
    public static final String ELEM_SIZE = "size";

    private final Date date;
    private final String desc;
    private final HashElement hash;
    private final String mediaType;
    private final String name;
    private final int size;
    private final Range range;

    public FileTransferPayload(Date date, String desc, HashElement hash, String mediaType, String name, int size, Range range) {
        this.date = date;
        this.desc = desc;
        this.hash = hash;
        this.mediaType = mediaType;
        this.name = name;
        this.size = size;
        this.range = range;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return desc;
    }

    public HashElement getHash() {
        return hash;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public Range getRange() {
        return range;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this);
        sb.rightAngleBracket();

        if (date != null) {
            sb.element(ELEM_DATE, date);
        }

        if (desc != null) {
            sb.element(ELEM_DESC, desc);
        }

        if (mediaType != null) {
            sb.element(ELEM_MEDIA_TYPE, mediaType);
        }

        if (name != null) {
            sb.element(ELEM_NAME, name);
        }

        if (range != null) {
            sb.element(range);
        }

        if (size > 0) {
            sb.element(ELEM_SIZE, Integer.toString(size));
        }

        if (hash != null) {
            sb.element(hash);
        }

        sb.closeElement(this);
        return sb;
    }

}
