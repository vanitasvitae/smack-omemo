package org.jivesoftware.smackx.ox.bouncycastle;

import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import de.vanitasvitae.crypto.pgpainless.PGPainless;
import de.vanitasvitae.crypto.pgpainless.key.UnprotectedKeysProtector;
import de.vanitasvitae.crypto.pgpainless.key.generation.type.length.RsaLength;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class FileBasedPainlessOpenPgpStoreTest extends SmackTestSuite {

    private static final File basePath;
    private static final BareJid alice;
    private static final BareJid bob;

    static {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            basePath = new File(f, ".config/smack-integration-test/ox/painless_ox_store");
        } else {
            basePath = new File("painless_ox_store");
        }

        try {
            alice = JidCreate.bareFrom("alice@wonderland.lit");
            bob = JidCreate.bareFrom("bob@builder.tv");
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void storeSecretKeyRingsTest()
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException,
            IOException {
        FileBasedPainlessOpenPgpStore store = new FileBasedPainlessOpenPgpStore(basePath, new UnprotectedKeysProtector());

        PGPSecretKeyRing secretKey = PGPainless.generateKeyRing().simpleRsaKeyRing("xmpp:" + alice.toString(), RsaLength._3072);
        PGPSecretKeyRingCollection saving = new PGPSecretKeyRingCollection(Collections.singleton(secretKey));
        store.storeSecretKeyRing(alice, saving);

        PGPSecretKeyRingCollection restored = store.getSecretKeyRings(alice);

        assertTrue(Arrays.equals(saving.getEncoded(), restored.getEncoded()));
    }
}
