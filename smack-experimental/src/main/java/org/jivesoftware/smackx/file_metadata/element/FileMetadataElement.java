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
package org.jivesoftware.smackx.file_metadata.element;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;

/**
 * File metadata element as defined in XEP-0446: File Metadata Element.
 *
 * This element is used in a generic way to provide information about files, eg. during file sharing.
 */
public final class FileMetadataElement implements ExtensionElement {

    public static final String ELEMENT = "file";
    public static final String NAMESPACE = "urn:xmpp:file:metadata:0";
    public static final String ELEM_DATE = "date";
    public static final String ELEM_DIMENSIONS = "dimensions";
    public static final String ELEM_DESC = "desc";
    public static final String ELEM_LENGTH = "length";
    public static final String ELEM_MEDIA_TYPE = "media-type";
    public static final String ELEM_NAME = "name";
    public static final String ELEM_SIZE = "size";
    public static final String ELEM_THUMBNAIL = "thumbnail";


    private final Date date;
    private final String dimensions;
    private final Map<String, String> descriptions = new ConcurrentHashMap<>();
    private final Map<HashManager.ALGORITHM, HashElement> hashElements = new ConcurrentHashMap<>();
    private final Long length;
    private final String mediaType;
    private final String name;
    private final Long size;
    private final Set<ExtensionElement> otherElements = new HashSet<>();

    private FileMetadataElement(Date date, String dimensions, Map<String, String> descriptions,
                               Map<HashManager.ALGORITHM, HashElement> hashElements, Long length,
                               String mediaType, String name, Long size,
                               Set<ExtensionElement> otherElements) {
        this.date = date;
        this.dimensions = dimensions;
        this.descriptions.putAll(descriptions);
        this.hashElements.putAll(hashElements);
        this.length = length;
        this.mediaType = mediaType;
        this.name = name;
        this.size = size;
        this.otherElements.addAll(otherElements);
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this)
                .rightAngleBracket()
                .optElement(ELEM_DATE, date)
                .optElement(ELEM_DIMENSIONS, dimensions);
        for (String key : descriptions.keySet()) {
            sb.halfOpenElement(ELEM_DESC)
                    .optXmlLangAttribute(key)
                    .rightAngleBracket()
                    .append(descriptions.get(key))
                    .closeElement(ELEM_DESC);
        }
        sb.append(hashElements.values())
                .optElement(ELEM_LENGTH, length != null ? Long.toString(length) : null)
                .optElement(ELEM_MEDIA_TYPE, mediaType)
                .optElement(ELEM_NAME, name)
                .optElement(ELEM_SIZE, size != null ? Long.toString(size) : null)
                .append(otherElements);
        return sb.closeElement(this);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public Date getDate() {
        return date;
    }

    public String getDimensions() {
        return dimensions;
    }

    public Map<String, String> getDescriptions() {
        return new ConcurrentHashMap<>(descriptions);
    }

    public String getDescription() {
        return getDescription(getLanguage());
    }

    public String getDescription(String lang) {
        return descriptions.get(lang != null ? lang : "");
    }

    public Map<HashManager.ALGORITHM, HashElement> getHashElements() {
        return new ConcurrentHashMap<>(hashElements);
    }

    public HashElement getHashElement(HashManager.ALGORITHM algorithm) {
        return hashElements.get(algorithm);
    }

    public Long getLength() {
        return length;
    }

    public String getMediaType() {
        return mediaType;
    }

    /**
     * Return the name of the file.
     *
     * @return escaped name
     */
    public String getName() {
        if (name == null) {
            return null;
        }
        try {
            return URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e); // UTF-8 MUST be supported
        }
    }

    public String getRawName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    public Set<ExtensionElement> getThumbnails() {
        Set<ExtensionElement> thumbnails = new HashSet<>();
        for (ExtensionElement e : otherElements) {
            if (isThumbnail(e)) {
                thumbnails.add(e);
            }
        }
        return thumbnails;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getNamespace())
                .append(getDate())
                .append(getDescriptions())
                .append(getDimensions())
                .append(getHashElements())
                .append(getLength())
                .append(getMediaType())
                .append(getRawName())
                .append(getSize())
                .append(getThumbnails())
                .build();
    }

    @Override
    public boolean equals(Object other) {
        return EqualsUtil.equals(this, other, (equalsBuilder, o) -> equalsBuilder
                .append(getElementName(), o.getElementName())
                .append(getNamespace(), o.getNamespace())
                .append(getDate(), o.getDate())
                .append(getDescriptions(), o.getDescriptions())
                .append(getDimensions(), o.getDimensions())
                .append(getHashElements(), o.getHashElements())
                .append(getLength(), o.getLength())
                .append(getMediaType(), o.getMediaType())
                .append(getRawName(), o.getRawName())
                .append(getSize(), o.getSize())
                .append(getThumbnails(), o.getThumbnails()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Date date;
        private String dimensions;
        private Map<String, String> descriptions = new ConcurrentHashMap<>();
        private Map<HashManager.ALGORITHM, HashElement> hashElements = new ConcurrentHashMap<>();
        private Long length;
        private String mediaType;
        private String name;
        private Long size;
        private Set<ExtensionElement> thumbnails = new HashSet<>();

        public Builder setModificationDate(Date date) {
            this.date = date;
            return this;
        }

        public Builder setDimensions(int width, int height) {
            if (width <= 0) {
                throw new IllegalArgumentException("Width must be a positive number");
            }
            if (height <= 0) {
                throw new IllegalArgumentException("Height must be a positive number");
            }
            return setDimensions(width + "x" + height);
        }

        public Builder setDimensions(String dimenString) {
            this.dimensions = StringUtils.requireNotNullNorEmpty(dimenString, "Dimensions MUST NOT be null nor empty.");
            return this;
        }

        public Builder addDescription(String description) {
            return addDescription(description, null);
        }

        public Builder addDescription(String description, String language) {
            this.descriptions.put(language != null ? language : "", StringUtils.requireNotNullNorEmpty(description, "Description MUST NOT be null nor empty"));
            return this;
        }

        public Builder addHash(HashElement hashElement) {
            hashElements.put(hashElement.getAlgorithm(), hashElement);
            return this;
        }

        public Builder setLength(long length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative.");
            }
            this.length = length;
            return this;
        }

        public Builder setMediaType(String mediaType) {
            this.mediaType = StringUtils.requireNotNullNorEmpty(mediaType, "Media-Type MUST NOT be null nor empty");
            return this;
        }

        public Builder setName(String name) {
            this.name = StringUtils.requireNotNullNorEmpty(name, "Name MUST NOT be null nor empty");
            return this;
        }

        public Builder setSize(long size) {
            if (size < 0) {
                throw new IllegalArgumentException("Size MUST NOT be negative.");
            }
            this.size = size;
            return this;
        }

        public Builder addThumbnail(ExtensionElement element) {
            if (!isThumbnail(element)) {
                throw new IllegalArgumentException("Element must be a thumbnail element with namespace 'urn:xmpp:thumbs:1'.");
            }
            thumbnails.add(element);
            return this;
        }

        public FileMetadataElement build() {
            return new FileMetadataElement(date, dimensions, descriptions, hashElements, length,
                    mediaType, name, size, thumbnails);
        }
    }

    private static boolean isThumbnail(ExtensionElement element) {
        return element.getElementName().equals(ELEM_THUMBNAIL) && element.getNamespace().equals("urn:xmpp:thumbs:1");
    }
}
