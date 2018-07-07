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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.v2.store.AbstractOpenPgpKeyStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.PGPainless;


public class FileBasedOpenPgpKeyStore extends AbstractOpenPgpKeyStore {

    public static final String PUB_RING = "pubring.pkr";
    public static final String SEC_RING = "secring.skr";

    private final File basePath;

    public FileBasedOpenPgpKeyStore(File basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    @Override
    public void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException {
        File file = getPublicKeyRingPath(owner);
        OutputStream outputStream = prepareFileOutputStream(file);
        publicKeys.encode(outputStream);
        outputStream.close();
    }

    @Override
    public void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException {
        File file = getSecretKeyRingPath(owner);
        OutputStream outputStream = prepareFileOutputStream(file);
        secretKeys.encode(outputStream);
        outputStream.close();
    }

    @Override
    public PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner)
            throws IOException, PGPException {
        File file = getPublicKeyRingPath(owner);

        FileInputStream inputStream;
        try {
            inputStream = prepareFileInputStream(file);
        } catch (IOException e) {
            return null;
        }

        PGPPublicKeyRingCollection collection = PGPainless.readKeyRing().publicKeyRingCollection(inputStream);
        inputStream.close();
        return collection;
    }

    @Override
    public PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException {
        File file = getSecretKeyRingPath(owner);

        FileInputStream inputStream;
        try {
            inputStream = prepareFileInputStream(file);
        } catch (IOException e) {
            return null;
        }

        PGPSecretKeyRingCollection collection = PGPainless.readKeyRing().secretKeyRingCollection(inputStream);
        inputStream.close();
        return collection;
    }

    private File getPublicKeyRingPath(BareJid jid) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, jid), PUB_RING);
    }

    private File getSecretKeyRingPath(BareJid jid) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, jid), SEC_RING);
    }
}
