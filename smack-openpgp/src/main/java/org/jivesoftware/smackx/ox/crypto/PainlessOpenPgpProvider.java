package org.jivesoftware.smackx.ox.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpSelf;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.pgpainless.PGPainless;
import org.pgpainless.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.pgpainless.decryption_verification.PainlessResult;
import org.pgpainless.pgpainless.encryption_signing.EncryptionStream;

public class PainlessOpenPgpProvider implements OpenPgpProvider {

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
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[]{}))
                .andToSelf(self.getAnnouncedPublicKeys())
                .usingSecureAlgorithms()
                .signWith(null, self.getSigningKeyRing())
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
                .signWith(null, self.getSigningKeyRing())
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
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[]{}))
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

        DecryptionStream cipherStream = PGPainless.createDecryptor().onInputStream(cipherText)
                .decryptWith(null, self.getSecretKeys())
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
