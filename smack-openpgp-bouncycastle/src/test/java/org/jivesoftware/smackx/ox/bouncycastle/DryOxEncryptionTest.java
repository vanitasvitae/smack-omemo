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

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Collections;
import java.util.Date;

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.KeyBytesAndFingerprint;

import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;

public class DryOxEncryptionTest extends OxTestSuite {

    private static File getTempDir(String suffix) {
        String temp = System.getProperty("java.io.tmpdir");
        if (temp == null) {
            temp = "tmp";
        }

        if (suffix == null) {
            return new File(temp);
        } else {
            return new File(temp, suffix);
        }
    }

    @Test
    public void dryEncryptionTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, SmackOpenPgpException, MissingUserIdOnKeyException, MissingOpenPgpPublicKeyException {
        BareJid alice = JidTestUtil.BARE_JID_1;
        BareJid bob = JidTestUtil.BARE_JID_2;

        File alicePath = getTempDir("ox-alice");
        File bobPath = getTempDir("ox-bob");
        FileBasedPainlessOpenPgpStore aliceStore = new FileBasedPainlessOpenPgpStore(alicePath, new UnprotectedKeysProtector());
        FileBasedPainlessOpenPgpStore bobStore = new FileBasedPainlessOpenPgpStore(bobPath, new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(alice, aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(bob, bobStore);

        KeyBytesAndFingerprint aliceKey = aliceProvider.generateOpenPgpKeyPair(alice);
        KeyBytesAndFingerprint bobKey = bobProvider.generateOpenPgpKeyPair(bob);

        aliceProvider.importSecretKey(alice, aliceKey.getBytes());
        bobProvider.importSecretKey(bob, bobKey.getBytes());

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


    }
}
