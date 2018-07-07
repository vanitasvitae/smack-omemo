/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.v2.store.filebased;

import static org.jivesoftware.smackx.ox.util.FileUtils.prepareFileInputStream;
import static org.jivesoftware.smackx.ox.util.FileUtils.prepareFileOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.v2.store.AbstractOpenPgpMetadataStore;

import org.jxmpp.jid.BareJid;

public class FileBasedOpenPgpMetadataStore extends AbstractOpenPgpMetadataStore {

    public static final String METADATA = "announced.list";

    private final File basePath;

    public FileBasedOpenPgpMetadataStore(File basePath) {
        this.basePath = basePath;
    }

    @Override
    public Set<OpenPgpV4Fingerprint> readAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        InputStream inputStream = prepareFileInputStream(getMetadataPath(contact));
        return null;
    }

    @Override
    public void writeAnnouncedFingerprintsOf(BareJid contact, PublicKeysListElement metadata) throws IOException {
        OutputStream outputStream = prepareFileOutputStream(getMetadataPath(contact));
    }

    private File getMetadataPath(BareJid contact) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, contact), METADATA);
    }
}
