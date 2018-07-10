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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.pgpainless.key.protection.UnprotectedKeysProtector;
import org.pgpainless.pgpainless.util.BCUtil;
import org.xmlpull.v1.XmlPullParserException;

public class PainlessOpenPgpProviderTest extends SmackTestSuite {

    private static File storagePath;
    private static BareJid alice = JidTestUtil.BARE_JID_1;
    private static BareJid bob = JidTestUtil.BARE_JID_2;

    static {
        storagePath = FileUtils.getTempDir("smack-painlessprovidertest");
        Security.addProvider(new BouncyCastleProvider());
    }

    @BeforeClass
    @AfterClass
    public static void deletePath() {
        FileUtils.deleteDirectory(storagePath);
    }

    @Test
    public void signcryptTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException, XmlPullParserException {

        // Initialize

        OpenPgpStore aliceStore = new FileBasedOpenPgpStore(storagePath);
        OpenPgpStore bobStore = new FileBasedOpenPgpStore(storagePath);

        aliceStore.setKeyRingProtector(new UnprotectedKeysProtector());
        bobStore.setKeyRingProtector(new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(new DummyConnection(), aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(new DummyConnection(), bobStore);

        PGPSecretKeyRing aliceKeys = aliceStore.generateKeyRing(alice);
        PGPSecretKeyRing bobKeys = bobStore.generateKeyRing(bob);

        aliceStore.importSecretKey(alice, aliceKeys);
        bobStore.importSecretKey(bob, bobKeys);

        aliceStore.setAnnouncedFingerprintsOf(alice, Collections.singletonMap(new OpenPgpV4Fingerprint(aliceKeys), new Date()));
        bobStore.setAnnouncedFingerprintsOf(bob, Collections.singletonMap(new OpenPgpV4Fingerprint(bobKeys), new Date()));

        OpenPgpSelf aliceSelf = new OpenPgpSelf(alice, aliceStore);
        OpenPgpSelf bobSelf = new OpenPgpSelf(bob, bobStore);

        // Exchange keys

        aliceStore.importPublicKey(bob, BCUtil.publicKeyRingFromSecretKeyRing(bobKeys));
        bobStore.importPublicKey(alice, BCUtil.publicKeyRingFromSecretKeyRing(aliceKeys));

        aliceStore.setAnnouncedFingerprintsOf(bob, Collections.singletonMap(new OpenPgpV4Fingerprint(bobKeys), new Date()));
        bobStore.setAnnouncedFingerprintsOf(alice, Collections.singletonMap(new OpenPgpV4Fingerprint(aliceKeys), new Date()));

        OpenPgpContact aliceForBob = new OpenPgpContact(alice, bobStore);
        OpenPgpContact bobForAlice = new OpenPgpContact(bob, aliceStore);

        // Prepare message

        Message.Body body = new Message.Body(null, "Lorem ipsum dolor sit amet, consectetur adipisici elit, sed eiusmod tempor incidunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquid ex ea commodi consequat. Quis aute iure reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint obcaecat cupiditat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        List<ExtensionElement> payload = Collections.<ExtensionElement>singletonList(body);

        SigncryptElement signcryptElement = new SigncryptElement(Collections.<Jid>singleton(bob), payload);

        // Encrypt and Sign
        OpenPgpElement encrypted = aliceProvider.signAndEncrypt(signcryptElement, aliceSelf, Collections.singleton(bobForAlice));

        // Decrypt and Verify
        OpenPgpMessage decrypted = bobProvider.decryptAndOrVerify(encrypted, bobSelf, aliceForBob);

        assertEquals(decrypted.getMetadata().getDecryptionFingerprint(), new OpenPgpV4Fingerprint(bobKeys));
        assertTrue(decrypted.getMetadata().getValidSignatureFingerprints().contains(new OpenPgpV4Fingerprint(aliceKeys)));

        assertEquals(OpenPgpMessage.State.signcrypt, decrypted.getState());
        SigncryptElement decryptedVerified = (SigncryptElement) decrypted.getOpenPgpContentElement();

        assertEquals(body.getMessage(), decryptedVerified.<Message.Body>getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE).getMessage());

    }
}
