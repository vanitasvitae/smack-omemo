/**
 *
 * Copyright 2017 Florian Schmaus.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpProvider;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.Xep0373KeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class BouncycastleOpenPgpProvider implements OpenPgpProvider {

    public static final Provider PROVIDER = new BouncyCastleProvider();

    private final KeyringConfig keyringConfig;
    private final BareJid signingIdentity;
    private final KeySelectionStrategy keySelectionStrategy = new Xep0373KeySelectionStrategy(new Date());

    public BouncycastleOpenPgpProvider(KeyringConfig config, BareJid signingIdentity) throws IOException, PGPException {
        this.keyringConfig = config;
        this.signingIdentity = signingIdentity;
    }

    public OpenPgpMessage toOpenPgpMessage(InputStream is, Set<Jid> recipients, Jid signer)
            throws IOException, PGPException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException {

        String[] to = new String[recipients.size()];
        Iterator<Jid> it = recipients.iterator();
        for (int i = 0; i<recipients.size(); i++) {
            to[i] = "xmpp:" + it.next().asBareJid().toString();
        }

        OutputStream resultStream = new ByteArrayOutputStream();

        OutputStream os = BouncyGPG.encryptToStream()
                .withConfig(keyringConfig)
                .withKeySelectionStrategy(keySelectionStrategy)
                .withOxAlgorithms()
                .toRecipients(to)
                .andSignWith("xmpp:" + signer.asBareJid().toString())
                .binaryOutput()
                .andWriteTo(resultStream);

        Streams.pipeAll(is, os);
        os.close();

        byte[] encrypted = ((ByteArrayOutputStream) resultStream).toByteArray();
        return new OpenPgpMessage(OpenPgpMessage.State.signcrypt, new String(encrypted, Charset.forName("UTF-8")));
    }

    public static PGPSecretKey generateKey(BareJid owner, char[] passPhrase) throws NoSuchAlgorithmException, PGPException {
        // Create RSA Key Pair
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", PROVIDER);
        generator.initialize(2048);
        KeyPair rsaPair = generator.generateKeyPair();

        PGPDigestCalculator calculator = new JcaPGPDigestCalculatorProviderBuilder()
                .setProvider(PROVIDER)
                .build()
                .get(HashAlgorithmTags.SHA256);

        PGPKeyPair pgpPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, rsaPair, new Date());
        PGPSecretKey secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION,
                pgpPair, "xmpp:" + owner.toString(), calculator, null, null,
                new JcaPGPContentSignerBuilder(pgpPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256),
                new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, calculator)
                        .setProvider(PROVIDER).build(passPhrase));

        return secretKey;
    }

    @Override
    public OpenPgpMessage toOpenPgpMessage(InputStream is) {
        return null;
    }
}
