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

import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import de.vanitasvitae.crypto.pgpainless.PGPainless;
import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;

public class UserIdTest extends OxTestSuite {

    private final File path;

    public UserIdTest() {
        this.path = FileUtils.getTempDir("ox-user-id-test");
    }

    @After
    @Before
    public void deleteDir() {
        FileUtils.deleteDirectory(path);
    }

    @Test
    public void requireUserIdOnImportTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, SmackOpenPgpException, MissingUserIdOnKeyException {
        BareJid owner = JidTestUtil.BARE_JID_1;
        FileBasedPainlessOpenPgpStore store = new FileBasedPainlessOpenPgpStore(path, new UnprotectedKeysProtector());
        PainlessOpenPgpProvider provider = new PainlessOpenPgpProvider(owner, store);
        PGPSecretKeyRing ownerKey = PGPainless.generateKeyRing().simpleEcKeyRing("xmpp:" + owner.toString());
        provider.importSecretKey(owner, ownerKey.getEncoded());
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void throwOnMissingUserIdTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException, SmackOpenPgpException, MissingUserIdOnKeyException {
        BareJid owner = JidTestUtil.BARE_JID_1;
        BareJid stranger = JidTestUtil.BARE_JID_2;
        FileBasedPainlessOpenPgpStore store = new FileBasedPainlessOpenPgpStore(path, new UnprotectedKeysProtector());
        PainlessOpenPgpProvider provider = new PainlessOpenPgpProvider(owner, store);
        PGPSecretKeyRing strangerKey = PGPainless.generateKeyRing().simpleEcKeyRing("xmpp:" + stranger.toString());
        provider.importSecretKey(owner, strangerKey.getEncoded());
    }
}
