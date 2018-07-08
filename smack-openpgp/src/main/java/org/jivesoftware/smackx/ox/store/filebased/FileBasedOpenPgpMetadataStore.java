/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.store.filebased;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.ox.store.abstr.AbstractOpenPgpMetadataStore;
import org.jivesoftware.smackx.ox.util.Util;

import org.bouncycastle.openpgp.PGPException;
import org.jxmpp.jid.BareJid;
import org.jxmpp.util.XmppDateTime;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;

public class FileBasedOpenPgpMetadataStore extends AbstractOpenPgpMetadataStore {

    public static final String ANNOUNCED = "announced.list";
    public static final String RETRIEVED = "retrieved.list";

    private static final Logger LOGGER = Logger.getLogger(FileBasedOpenPgpMetadataStore.class.getName());

    private final File basePath;

    public FileBasedOpenPgpMetadataStore(File basePath) {
        this.basePath = basePath;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> readAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        return readFingerprintsAndDates(getAnnouncedFingerprintsPath(contact));
    }

    @Override
    public void writeAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> metadata)
            throws IOException {
        File destination = getAnnouncedFingerprintsPath(contact);
        writeFingerprintsAndDates(metadata, destination);
    }

    private Map<OpenPgpV4Fingerprint, Date> readFingerprintsAndDates(File source) throws IOException {
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(source.toPath(), Util.UTF8);
            Map<OpenPgpV4Fingerprint, Date> fingerprintDateMap = new HashMap<>();

            String line; int lineNr = 0;
            while ((line = reader.readLine()) != null) {
                lineNr++;

                line = line.trim();
                String[] split = line.split(" ");
                if (split.length != 2) {
                    LOGGER.log(Level.FINE, "Skipping invalid line " + lineNr + " in file " + source.getAbsolutePath());
                    continue;
                }

                try {
                    OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(split[0]);
                    Date date = XmppDateTime.parseXEP0082Date(split[1]);
                    fingerprintDateMap.put(fingerprint, date);
                } catch (PGPException | ParseException e) {
                    LOGGER.log(Level.WARNING, "Error parsing fingerprint/date touple in line " + lineNr +
                            " of file " + source.getAbsolutePath(), e);
                }
            }

            reader.close();
            return fingerprintDateMap;

        } catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                    // Don't care
                }
            }
            throw e;
        }
    }

    private void writeFingerprintsAndDates(Map<OpenPgpV4Fingerprint, Date> data, File destination)
            throws IOException {

        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(destination.toPath(), Util.UTF8);
            for (OpenPgpV4Fingerprint fingerprint : data.keySet()) {
                Date date = data.get(fingerprint);
                String line = fingerprint.toString() + " " +
                        (date != null ? XmppDateTime.formatXEP0082Date(date) : XmppDateTime.formatXEP0082Date(new Date()));
                writer.write(line);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // Don't care
                }
            }
            throw e;
        }
    }

    private File getAnnouncedFingerprintsPath(BareJid contact) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, contact), ANNOUNCED);
    }

    private File getRetrievedFingerprintsPath(BareJid contact) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, contact), RETRIEVED);
    }
}
