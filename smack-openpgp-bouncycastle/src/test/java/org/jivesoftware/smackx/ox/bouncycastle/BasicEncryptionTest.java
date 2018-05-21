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

import static junit.framework.TestCase.assertTrue;
import static org.jivesoftware.smackx.ox.TestKeys.JULIET_UID;
import static org.jivesoftware.smackx.ox.TestKeys.ROMEO_UID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.TestKeys;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.XmppKeySelectionStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.InMemoryKeyring;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.util.io.Streams;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class BasicEncryptionTest extends SmackTestSuite {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final KeyringConfig keyringJuliet;
    private final KeyringConfig keyringRomeo;

    public BasicEncryptionTest() throws IOException, PGPException {
        super();

        // Prepare Juliet's keyring
        keyringJuliet = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
        ((InMemoryKeyring) keyringJuliet).addSecretKey(TestKeys.JULIET_PRIV.getBytes(UTF8));
        ((InMemoryKeyring) keyringJuliet).addPublicKey(TestKeys.JULIET_PUB.getBytes(UTF8));
        ((InMemoryKeyring) keyringJuliet).addPublicKey(TestKeys.ROMEO_PUB.getBytes(UTF8));

        // Prepare Romeos keyring
        keyringRomeo = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
        ((InMemoryKeyring) keyringRomeo).addSecretKey(TestKeys.ROMEO_PRIV.getBytes(UTF8));
        ((InMemoryKeyring) keyringRomeo).addPublicKey(TestKeys.ROMEO_PUB.getBytes(UTF8));
        ((InMemoryKeyring) keyringRomeo).addPublicKey(TestKeys.JULIET_PUB.getBytes(UTF8));
    }

    @Test
    public void encryptionTest()
            throws IOException, PGPException, NoSuchAlgorithmException, SignatureException, NoSuchProviderException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        KeySelectionStrategy selectionStrategy = new XmppKeySelectionStrategy(new Date());

        byte[] message = "How long do you want these messages to remain secret?".getBytes(UTF8);

        // Encrypt
        OutputStream out = BouncyGPG.encryptToStream()
                .withConfig(keyringJuliet)
                .withKeySelectionStrategy(selectionStrategy)
                .withOxAlgorithms()
                .toRecipients(ROMEO_UID, JULIET_UID)
                .andSignWith(JULIET_UID)
                .binaryOutput()
                .andWriteTo(result);

        out.write(message);
        out.close();

        byte[] encrypted = result.toByteArray();

        // Decrypt
        ByteArrayInputStream encIn = new ByteArrayInputStream(encrypted);
        InputStream in = BouncyGPG.decryptAndVerifyStream()
                .withConfig(keyringRomeo)
                .withKeySelectionStrategy(selectionStrategy)
                .andRequireSignatureFromAllKeys(JULIET_UID)
                .fromEncryptedInputStream(encIn);

        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        Streams.pipeAll(in, decrypted);

        byte[] message2 = decrypted.toByteArray();

        assertTrue(Arrays.equals(message, message2));
    }

    @Test
    public void encryptionWithFreshKeysTest()
            throws IOException, PGPException, NoSuchAlgorithmException, NoSuchProviderException, SignatureException {
        final String alice = "alice@wonderland.lit";
        final String bob = "bob@builder.tv";
        PGPKeyRingGenerator g1 = BouncyCastleOpenPgpProvider.generateKey(JidCreate.bareFrom(alice));
        PGPKeyRingGenerator g2 = BouncyCastleOpenPgpProvider.generateKey(JidCreate.bareFrom(bob));
        PGPSecretKey s1 = g1.generateSecretKeyRing().getSecretKey();
        PGPSecretKey s2 = g2.generateSecretKeyRing().getSecretKey();
        PGPPublicKey p1 = g1.generatePublicKeyRing().getPublicKey();
        PGPPublicKey p2 = g2.generatePublicKeyRing().getPublicKey();

        KeyringConfig c1 = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
        KeyringConfig c2 = KeyringConfigs.forGpgExportedKeys(KeyringConfigCallbacks.withUnprotectedKeys());
        ((InMemoryKeyring) c1).addSecretKey(s1.getEncoded());
        ((InMemoryKeyring) c1).addPublicKey(p1.getEncoded());
        ((InMemoryKeyring) c1).addPublicKey(p2.getEncoded());
        ((InMemoryKeyring) c2).addSecretKey(s2.getEncoded());
        ((InMemoryKeyring) c2).addPublicKey(p2.getEncoded());
        ((InMemoryKeyring) c2).addPublicKey(p1.getEncoded());

        ByteArrayOutputStream result = new ByteArrayOutputStream();

        byte[] m1 = "I want them to remain secret for as long as men are capable of evil.".getBytes(UTF8);
        OutputStream encrypt = BouncyGPG.encryptToStream()
                .withConfig(c1)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .withOxAlgorithms()
                .toRecipients("xmpp:" + alice, "xmpp:" + bob)
                .andSignWith("xmpp:" + alice)
                .binaryOutput()
                .andWriteTo(result);

        encrypt.write(m1);
        encrypt.close();

        byte[] e1 = result.toByteArray();
        result.reset();

        ByteArrayInputStream in = new ByteArrayInputStream(e1);
        InputStream decrypt = BouncyGPG.decryptAndVerifyStream()
                .withConfig(c2)
                .withKeySelectionStrategy(new XmppKeySelectionStrategy(new Date()))
                .andValidateSomeoneSigned()
                .fromEncryptedInputStream(in);

        Streams.pipeAll(decrypt, result);

        byte[] m2 = result.toByteArray();
        assertTrue(Arrays.equals(m1, m2));
    }
}
