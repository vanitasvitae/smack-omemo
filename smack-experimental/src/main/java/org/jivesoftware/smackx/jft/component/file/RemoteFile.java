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

import java.util.Date;

import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class RemoteFile extends AbstractJingleFileTransferFile {

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
