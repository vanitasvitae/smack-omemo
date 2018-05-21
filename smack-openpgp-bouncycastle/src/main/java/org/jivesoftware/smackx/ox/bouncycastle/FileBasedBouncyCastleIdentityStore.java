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
package org.jivesoftware.smackx.ox.bouncycastle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.jxmpp.util.XmppDateTime;

public class FileBasedBouncyCastleIdentityStore implements BouncyCastleIdentityStore {

    private static final Logger LOGGER = Logger.getLogger(FileBasedBouncyCastleIdentityStore.class.getName());

    private final File baseDirectory;

    public FileBasedBouncyCastleIdentityStore(File path) {
        // Check if path is not null, not a file and directory exists
        if (!Objects.requireNonNull(path).exists()) {
            path.mkdirs();
        } else if (path.isFile()) {
            throw new IllegalArgumentException("Path MUST point to a directory, not a file.");
        }

        this.baseDirectory = path;
    }

    @Override
    public void storePubkeyList(BareJid jid, PublicKeysListElement list) throws IOException {
        File contactsDir = contactsDir(jid);
        File destination = new File(contactsDir, "pubkey_list");
        DataOutputStream dataOut = new DataOutputStream(new FileOutputStream(destination));

        for (String key : list.getMetadata().keySet()) {
            PublicKeysListElement.PubkeyMetadataElement value = list.getMetadata().get(key);
            dataOut.writeUTF(value.getV4Fingerprint());
            dataOut.writeUTF(XmppDateTime.formatXEP0082Date(value.getDate()));
        }

        dataOut.close();
    }

    @Override
    public PublicKeysListElement loadPubkeyList(BareJid jid) throws IOException {
        File contactsDir = contactsDir(jid);
        File source = new File(contactsDir, "pubkey_list");
        if (!source.exists()) {
            LOGGER.log(Level.FINE, "File " + source.getAbsolutePath() + " does not exist. Returning null.");
            return null;
        }
        DataInputStream dataIn = new DataInputStream(new FileInputStream(source));

        PublicKeysListElement.Builder builder = PublicKeysListElement.builder();
        while (true) {
            try {
                String fingerprint = dataIn.readUTF();
                String d = dataIn.readUTF();
                Date date = XmppDateTime.parseXEP0082Date(d);
                builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fingerprint, date));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, "Could not parse date.", e);
            } catch (EOFException e) {
                LOGGER.log(Level.INFO, "Reached EOF.");
                break;
            }
        }
        return builder.build();
    }

    @Override
    public void storePublicKeys(BareJid jid, PGPPublicKeyRingCollection keys) {

    }

    @Override
    public PGPPublicKeyRingCollection loadPublicKeys(BareJid jid) {
        return null;
    }

    @Override
    public void storeSecretKeys(PGPSecretKeyRingCollection secretKeys) {

    }

    @Override
    public PGPSecretKeyRingCollection loadSecretKeys() {
        return null;
    }

    private File contactsDir(BareJid contact) {
        File f = new File(baseDirectory, "contacts/" + contact.toString());
        if (!f.exists()) {
            f.mkdirs();
        }
        return f;
    }
}
