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
package org.jivesoftware.smackx.ox.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpSelf;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.pubsub.PubSubException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.pgpainless.PGPainless;
import org.pgpainless.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.pgpainless.decryption_verification.PainlessResult;
import org.pgpainless.pgpainless.encryption_signing.EncryptionStream;

public class PainlessOpenPgpProvider implements OpenPgpProvider {

    private static final Logger LOGGER = Logger.getLogger(PainlessOpenPgpProvider.class.getName());

    private final XMPPConnection connection;
    private final OpenPgpStore store;

    public PainlessOpenPgpProvider(XMPPConnection connection, OpenPgpStore store) {
        this.connection = connection;
        this.store = store;
    }

    @Override
    public OpenPgpStore getStore() {
        return store;
    }

    @Override
    public OpenPgpElement signAndEncrypt(SigncryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        ArrayList<PGPPublicKeyRingCollection> recipientKeys = new ArrayList<>();
        for (OpenPgpContact contact : recipients) {
            recipientKeys.add(contact.getAnnouncedPublicKeys());
        }

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[] {}))
                .andToSelf(self.getAnnouncedPublicKeys())
                .usingSecureAlgorithms()
                .signWith(store.getKeyRingProtector(), self.getSigningKeyRing())
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        // TODO: Return feedback about encryption?
        return new OpenPgpElement(base64);
    }

    @Override
    public OpenPgpElement sign(SignElement element, OpenPgpSelf self)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .doNotEncrypt()
                .signWith(store.getKeyRingProtector(), self.getSigningKeyRing())
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        return new OpenPgpElement(base64);
    }

    @Override
    public OpenPgpElement encrypt(CryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        ArrayList<PGPPublicKeyRingCollection> recipientKeys = new ArrayList<>();
        for (OpenPgpContact contact : recipients) {
            recipientKeys.add(contact.getAnnouncedPublicKeys());
        }

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[] {}))
                .andToSelf(self.getAnnouncedPublicKeys())
                .usingSecureAlgorithms()
                .doNotSign()
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        // TODO: Return feedback about encryption?
        return new OpenPgpElement(base64);
    }

    @Override
    public OpenPgpMessage decryptAndOrVerify(OpenPgpElement element, OpenPgpSelf self, OpenPgpContact sender) throws IOException, PGPException {
        ByteArrayOutputStream plainText = new ByteArrayOutputStream();
        InputStream cipherText = element.toInputStream();

        PGPPublicKeyRingCollection announcedPublicKeys = sender.getAnnouncedPublicKeys();
        if (announcedPublicKeys == null) {
            try {
                sender.updateKeys(connection);
                announcedPublicKeys = sender.getAnnouncedPublicKeys();
            } catch (InterruptedException | PubSubException.NotAPubSubNodeException | PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException | SmackException.NoResponseException | SmackException.NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Error fetching keys of " + sender.getJid(), e);
            }
        }

        DecryptionStream cipherStream = PGPainless.createDecryptor().onInputStream(cipherText)
                .decryptWith(store.getKeyRingProtector(), self.getSecretKeys())
                .verifyWith(sender.getAnnouncedPublicKeys())
                .ignoreMissingPublicKeys()
                .build();

        Streams.pipeAll(cipherStream, plainText);

        cipherText.close();
        cipherStream.close();
        plainText.close();

        PainlessResult info = cipherStream.getResult();

        return new OpenPgpMessage(plainText.toByteArray(), new OpenPgpMessage.Metadata(
                info.getDecryptionFingerprint(), info.getVerifiedSignaturesFingerprints()));
    }
}
