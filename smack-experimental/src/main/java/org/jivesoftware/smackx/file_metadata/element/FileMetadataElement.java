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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.file_metadata.element.child.DateElement;
import org.jivesoftware.smackx.file_metadata.element.child.DescElement;
import org.jivesoftware.smackx.file_metadata.element.child.DimensionsElement;
import org.jivesoftware.smackx.file_metadata.element.child.LengthElement;
import org.jivesoftware.smackx.file_metadata.element.child.MediaTypeElement;
import org.jivesoftware.smackx.file_metadata.element.child.NameElement;
import org.jivesoftware.smackx.file_metadata.element.child.SizeElement;
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

    private final Set<NamedElement> children = new HashSet<>();

    private FileMetadataElement(Collection<NamedElement> childElements) {
        this.children.addAll(childElements);
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();
        for (Element child : children) {
            sb.append(child);
        }
        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public Set<NamedElement> getAllChildren() {
        return Collections.unmodifiableSet(children);
    }

    private NamedElement getChildElement(String name, String namespace) {
        for (NamedElement element : getAllChildren()) {
            if (!element.getElementName().equals(name)) {
                continue;
            }

            if (namespace == null && !(element instanceof ExtensionElement)) {
                return element;
            }

            ExtensionElement extensionElement = (ExtensionElement) element;
            if (extensionElement.getNamespace().equals(namespace)) {
                return extensionElement;
            }
        }
        return null;
    }

    public DateElement getDateElement() {
        return (DateElement) getChildElement(DateElement.ELEMENT, null);
    }

    public DimensionsElement getDimensionsElement() {
        return (DimensionsElement) getChildElement(DimensionsElement.ELEMENT, null);
    }

    public List<DescElement> getDescElements() {
        List<DescElement> elements = new ArrayList<>();
        for (NamedElement e : children) {
            if (e instanceof DescElement) {
                elements.add((DescElement) e);
            }
        }
        return elements;
    }

    public DescElement getDescElement() {
        return getDescElement(getLanguage());
    }

    public DescElement getDescElement(String lang) {
        List<DescElement> descElements = getDescElements();
        for (DescElement e : descElements) {
            if (Objects.equals(lang, e.getLanguage())) {
                return e;
            }
        }
        return null;
    }

    public List<HashElement> getHashElements() {
        List<HashElement> hashElements = new ArrayList<>();
        for (NamedElement e : children) {
            if (e instanceof HashElement) {
                hashElements.add((HashElement) e);
            }
        }
        return hashElements;
    }

    public HashElement getHashElement(HashManager.ALGORITHM algorithm) {
        List<HashElement> hashElements = getHashElements();
        for (HashElement e : hashElements) {
            if (e.getAlgorithm() == algorithm) {
                return e;
            }
        }
        return null;
    }

    public LengthElement getLengthElement() {
        return (LengthElement) getChildElement(LengthElement.ELEMENT, null);
    }

    public MediaTypeElement getMediaTypeElement() {
        return (MediaTypeElement) getChildElement(MediaTypeElement.ELEMENT, null);
    }

    public NameElement getNameElement() {
        return (NameElement) getChildElement(NameElement.ELEMENT, null);
    }

    public SizeElement getSizeElement() {
        return (SizeElement) getChildElement(SizeElement.ELEMENT, null);
    }

    public NamedElement getThumbnailElement() {
        return getChildElement("thumbnail", "urn:xmpp:thumbs:1");
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getNamespace())
                .append(getAllChildren())
                .build();
    }

    @Override
    public boolean equals(Object other) {
        return EqualsUtil.equals(this, other, (equalsBuilder, o) -> equalsBuilder
                .append(getElementName(), o.getElementName())
                .append(getNamespace(), o.getNamespace())
                .append(getAllChildren(), o.getAllChildren()));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<NamedElement> children = new HashSet<>();

        public Builder setModificationDate(Date date) {
            children.add(new DateElement(date));
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
            children.add(new DimensionsElement(dimenString));
            return this;
        }

        public Builder addDescription(String description) {
            return addDescription(description, null);
        }

        public Builder addDescription(String description, String language) {
            children.add(new DescElement(description, language));
            return this;
        }

        public Builder addHash(HashElement hashElement) {
            children.add(Objects.requireNonNull(hashElement));
            return this;
        }

        public Builder setLength(long length) {
            children.add(new LengthElement(length));
            return this;
        }

        public Builder setMediaType(String mediaType) {
            children.add(new MediaTypeElement(mediaType));
            return this;
        }

        public Builder setName(String name) {
            children.add(new NameElement(name));
            return this;
        }

        public Builder setSize(long size) {
            children.add(new SizeElement(size));
            return this;
        }

        public FileMetadataElement build() {
            return new FileMetadataElement(children);
        }
    }
}
