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
package org.jivesoftware.smackx.jft.component.file;

import java.io.File;
import java.util.Date;

import org.jivesoftware.smackx.hashes.element.HashElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class LocalFile extends AbstractJingleFileTransferFile {

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
