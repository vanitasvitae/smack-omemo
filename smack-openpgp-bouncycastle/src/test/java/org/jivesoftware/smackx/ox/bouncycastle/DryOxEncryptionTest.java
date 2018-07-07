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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.TestKeys;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import org.bouncycastle.util.encoders.Base64;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.protection.UnprotectedKeysProtector;
import org.pgpainless.pgpainless.util.BCUtil;
import org.xmlpull.v1.XmlPullParserException;


public class DryOxEncryptionTest extends OxTestSuite {

    private static final Logger LOGGER = Logger.getLogger(DryOxEncryptionTest.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final File julietPath = FileUtils.getTempDir("ox-juliet");
    private static final File romeoPath = FileUtils.getTempDir("ox-romeo");

    @BeforeClass
    @AfterClass
    public static void deletePath() {
        LOGGER.log(Level.INFO, "Delete paths " + julietPath.getAbsolutePath() + " " + romeoPath.getAbsolutePath());
        FileUtils.deleteDirectory(julietPath);
        FileUtils.deleteDirectory(romeoPath);
    }

    // @Test
    public void dryEncryptionTest()
            throws IOException, SmackOpenPgpException, MissingUserIdOnKeyException, MissingOpenPgpPublicKeyException,
            MissingOpenPgpKeyPairException, XmlPullParserException, SmackException.NotLoggedInException {
        BareJid julietJid = TestKeys.JULIET_JID;
        BareJid romeoJid = TestKeys.ROMEO_JID;

        XMPPConnection julietCon = new DummyConnection();
        XMPPConnection romeoCon = new DummyConnection();

        FileBasedPainlessOpenPgpStore julietStore = new FileBasedPainlessOpenPgpStore(julietPath, new UnprotectedKeysProtector());
        FileBasedPainlessOpenPgpStore romeoStore = new FileBasedPainlessOpenPgpStore(romeoPath, new UnprotectedKeysProtector());

        PainlessOpenPgpProvider julietProvider = new PainlessOpenPgpProvider(julietJid, julietStore);
        PainlessOpenPgpProvider romeoProvider = new PainlessOpenPgpProvider(romeoJid, romeoStore);

        OpenPgpV4Fingerprint julietFinger = julietProvider.importSecretKey(julietJid,
                BCUtil.getDecodedBytes(TestKeys.JULIET_PRIV.getBytes(UTF8)));
        OpenPgpV4Fingerprint romeoFinger = romeoProvider.importSecretKey(romeoJid,
                BCUtil.getDecodedBytes(TestKeys.ROMEO_PRIV.getBytes(UTF8)));

        julietStore.setSigningKeyPairFingerprint(julietFinger);
        romeoStore.setSigningKeyPairFingerprint(romeoFinger);

        byte[] julietPubBytes = julietStore.getPublicKeyRingBytes(julietJid, julietFinger);
        byte[] romeoPubBytes = romeoStore.getPublicKeyRingBytes(romeoJid, romeoFinger);

        assertNotNull(julietPubBytes);
        assertNotNull(romeoPubBytes);
        assertTrue(julietPubBytes.length != 0);
        assertTrue(romeoPubBytes.length != 0);

        PubkeyElement julietPub = new PubkeyElement(new PubkeyElement.PubkeyDataElement(
                Base64.encode(julietStore.getPublicKeyRingBytes(julietJid, julietFinger))),
                new Date());
        PubkeyElement romeoPub = new PubkeyElement(new PubkeyElement.PubkeyDataElement(
                Base64.encode(romeoStore.getPublicKeyRingBytes(romeoJid, romeoFinger))),
                new Date());

        julietProvider.importPublicKey(romeoJid, Base64.decode(romeoPub.getDataElement().getB64Data()));
        romeoProvider.importPublicKey(julietJid, Base64.decode(julietPub.getDataElement().getB64Data()));

        julietStore.setAnnouncedKeysFingerprints(romeoJid, Collections.singletonMap(romeoFinger, new Date()));
        romeoStore.setAnnouncedKeysFingerprints(julietJid, Collections.singletonMap(julietFinger, new Date()));

        OpenPgpContact julietForRomeo = new OpenPgpContact(romeoProvider, julietJid, romeoCon);
        OpenPgpContact romeoForJuliet = new OpenPgpContact(julietProvider, romeoJid, julietCon);

        String bodyText = "Finden wir eine Kompromisslösung – machen wir es so, wie ich es sage.";
        List<ExtensionElement> payload = Collections.<ExtensionElement>singletonList(new Message.Body("de",
                        bodyText));

        OpenPgpElement encrypted = romeoForJuliet.encryptAndSign(payload);

        LOGGER.log(Level.INFO, encrypted.toXML(null).toString());

        OpenPgpContentElement decrypted = julietForRomeo.receive(encrypted);
        assertTrue(decrypted instanceof SigncryptElement);

        assertEquals(1, decrypted.getExtensions().size());
        Message.Body body = (Message.Body) decrypted.getExtensions().get(0);
        assertEquals(bodyText, body.getMessage());
    }
}
