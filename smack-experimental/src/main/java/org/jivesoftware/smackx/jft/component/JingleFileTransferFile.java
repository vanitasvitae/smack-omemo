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
package org.jivesoftware.smackx.jft.component;

import java.io.File;
import java.util.Date;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Represent a file sent in a file transfer.
 * This can be both LocalFile (available to the client), or RemoteFile (file not yet available).
 */
public abstract class JingleFileTransferFile {

    public JingleFileTransferFile() {

    }

    public JingleFileTransferChildElement getElement() {
        JingleFileTransferChildElement.Builder builder = JingleFileTransferChildElement.getBuilder();
        builder.setDate(getDate());
        builder.setSize(getSize());
        builder.setName(getName());
        builder.setDescription(getDescription());
        builder.setMediaType(getMediaType());
        builder.setHash(getHashElement());

        return builder.build();
    }

    public abstract Date getDate();

    public abstract long getSize();

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getMediaType();

    public abstract HashElement getHashElement();

    public static class LocalFile extends JingleFileTransferFile {

        private File file;
        private String name;
        private String description;
        private String mediaType;
        private HashElement hashElement;

        public LocalFile(File file) {
            this(file, null, null);
        }

        public LocalFile(File file, String description) {
            this(file, description, null);
        }

        public LocalFile(File file, String description, String mediaType) {
            super();
            this.file = file;
            String path = file.getAbsolutePath();
            name = path.substring(path.lastIndexOf(File.separator) + 1);
            this.description = description;
            this.mediaType = mediaType;
        }

        @Override
        public Date getDate() {
            return new Date(file.lastModified());
        }

        public void setDate(Date date) {
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getMediaType() {
            return mediaType;
        }

        @Override
        public HashElement getHashElement() {
            return hashElement;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public void setHashElement(HashElement hashElement) {
            this.hashElement = hashElement;
        }

        public File getFile() {
            return file;
        }
    }

    public static class RemoteFile extends JingleFileTransferFile {

        private JingleFileTransferChildElement file;

        public RemoteFile(JingleFileTransferChildElement file) {
            super();
            this.file = file;
        }

        @Override
        public String getDescription() {
            return file.getDescription();
        }

        @Override
        public String getMediaType() {
            return file.getMediaType();
        }

        @Override
        public HashElement getHashElement() {
            return file.getHash();
        }

        @Override
        public Date getDate() {
            return file.getDate();
        }

        @Override
        public long getSize() {
            return file.getSize();
        }

        @Override
        public String getName() {
            return file.getName();
        }
    }

}
