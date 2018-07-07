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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.v2.store.OpenPgpTrustStore;

import org.jxmpp.jid.BareJid;

public class FileBasedOpenPgpTrustStore implements OpenPgpTrustStore {

    private final File basePath;

    public static String TRUST_RECORD(OpenPgpV4Fingerprint fingerprint) {
        return fingerprint.toString() + ".trust";
    }

    public FileBasedOpenPgpTrustStore(File basePath) {
        this.basePath = basePath;
    }

    @Override
    public Trust getTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        File file = getTrustPath(owner, fingerprint);
        try {
            InputStream inputStream = prepareFileInputStream(file);
            return Trust.trusted;

        } catch (IOException e) {
            return Trust.undecided;
        }
    }

    @Override
    public void setTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) {

    }

    private File getTrustPath(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, owner), TRUST_RECORD(fingerprint));
    }
}
