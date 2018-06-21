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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.chat.OpenPgpContact;
import org.jivesoftware.smackx.ox.chat.OpenPgpFingerprints;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.KeyBytesAndFingerprint;

import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;
import org.xmlpull.v1.XmlPullParserException;

public class DryOxEncryptionTest extends OxTestSuite {

    private static final Logger LOGGER = Logger.getLogger(DryOxEncryptionTest.class.getName());

    private static final File alicePath = FileUtils.getTempDir("ox-alice");
    private static final File bobPath = FileUtils.getTempDir("ox-bob");

    @BeforeClass
    @AfterClass
    public static void deletePath() {
        LOGGER.log(Level.INFO, "Delete paths " + alicePath.getAbsolutePath() + " " + bobPath.getAbsolutePath());
        FileUtils.deleteDirectory(alicePath);
        FileUtils.deleteDirectory(bobPath);
    }

    @Test
    public void dryEncryptionTest()
            throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, SmackOpenPgpException, MissingUserIdOnKeyException, MissingOpenPgpPublicKeyException,
            MissingOpenPgpKeyPairException, XmlPullParserException {
        BareJid alice = JidTestUtil.BARE_JID_1;
        BareJid bob = JidTestUtil.BARE_JID_2;

        FileBasedPainlessOpenPgpStore aliceStore = new FileBasedPainlessOpenPgpStore(alicePath, new UnprotectedKeysProtector());
        FileBasedPainlessOpenPgpStore bobStore = new FileBasedPainlessOpenPgpStore(bobPath, new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(alice, aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(bob, bobStore);

        KeyBytesAndFingerprint aliceKey = aliceProvider.generateOpenPgpKeyPair(alice);
        KeyBytesAndFingerprint bobKey = bobProvider.generateOpenPgpKeyPair(bob);

        aliceProvider.importSecretKey(alice, aliceKey.getBytes());
        bobProvider.importSecretKey(bob, bobKey.getBytes());

        byte[] alicePubBytes = aliceStore.getPublicKeyRingBytes(alice, aliceKey.getFingerprint());
        byte[] bobPubBytes = bobStore.getPublicKeyRingBytes(bob, bobKey.getFingerprint());

        assertNotNull(alicePubBytes);
        assertNotNull(bobPubBytes);
        assertTrue(alicePubBytes.length != 0);
        assertTrue(bobPubBytes.length != 0);

        PubkeyElement alicePub = new PubkeyElement(new PubkeyElement.PubkeyDataElement(
                Base64.encode(aliceStore.getPublicKeyRingBytes(alice, aliceKey.getFingerprint()))),
                new Date());
        PubkeyElement bobPub = new PubkeyElement(new PubkeyElement.PubkeyDataElement(
                Base64.encode(bobStore.getPublicKeyRingBytes(bob, bobKey.getFingerprint()))),
                new Date());

        aliceProvider.importPublicKey(bob, Base64.decode(bobPub.getDataElement().getB64Data()));
        bobProvider.importPublicKey(alice, Base64.decode(alicePub.getDataElement().getB64Data()));

        aliceStore.setAnnouncedKeysFingerprints(bob, Collections.singletonMap(bobKey.getFingerprint(), new Date()));
        bobStore.setAnnouncedKeysFingerprints(alice, Collections.singletonMap(aliceKey.getFingerprint(), new Date()));

        OpenPgpFingerprints aliceFingerprints = new OpenPgpFingerprints(alice,
                Collections.singleton(aliceKey.getFingerprint()),
                Collections.singleton(aliceKey.getFingerprint()),
                new HashMap<OpenPgpV4Fingerprint, Throwable>());
        OpenPgpFingerprints bobFingerprints = new OpenPgpFingerprints(bob,
                Collections.singleton(bobKey.getFingerprint()),
                Collections.singleton(bobKey.getFingerprint()),
                new HashMap<OpenPgpV4Fingerprint, Throwable>());

        OpenPgpContact aliceForBob = new OpenPgpContact(bobProvider, alice, bobFingerprints, aliceFingerprints);
        OpenPgpContact bobForAlice = new OpenPgpContact(aliceProvider, bob, aliceFingerprints, bobFingerprints);

        String bodyText = "Finden wir eine Kompromisslösung – machen wir es so, wie ich es sage.";
        List<ExtensionElement> payload = Collections.<ExtensionElement>singletonList(new Message.Body("de",
                        bodyText));

        OpenPgpElement encrypted = bobForAlice.encryptAndSign(payload);

        OpenPgpContentElement decrypted = aliceForBob.receive(encrypted);
        assertTrue(decrypted instanceof SigncryptElement);

        assertEquals(1, decrypted.getExtensions().size());
        Message.Body body = (Message.Body) decrypted.getExtensions().get(0);
        assertEquals(bodyText, body.getMessage());
    }
}
