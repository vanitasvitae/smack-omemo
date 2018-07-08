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
package org.jivesoftware.smackx.ox.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Set;

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.ox.crypto.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.PGPainless;
import org.pgpainless.pgpainless.algorithm.SymmetricKeyAlgorithm;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;

public class SecretKeyBackupHelper {

    /**
     * Generate a secure backup code.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#sect-idm140425111347232">XEP-0373 §5.3</a>
     * @return backup code
     */
    public static String generateBackupPassword() {
        final String alphabet = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        // 6 blocks
        for (int i = 0; i < 6; i++) {

            // of 4 chars
            for (int j = 0; j < 4; j++) {
                char c = alphabet.charAt(random.nextInt(alphabet.length()));
                code.append(c);
            }

            // dash after every block except the last one
            if (i != 5) {
                code.append('-');
            }
        }
        return code.toString();
    }

    public static SecretkeyElement createSecretkeyElement(OpenPgpProvider provider,
                                                    BareJid owner,
                                                    Set<OpenPgpV4Fingerprint> fingerprints,
                                                    String backupCode) throws PGPException, IOException, MissingOpenPgpKeyPairException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        for (OpenPgpV4Fingerprint fingerprint : fingerprints) {

                PGPSecretKeyRing key = provider.getStore().getSecretKeyRing(owner, fingerprint);
                if (key == null) {
                    throw new MissingOpenPgpKeyPairException(owner, fingerprint);
                }

                byte[] bytes = key.getEncoded();
                buffer.write(bytes);
        }
        return createSecretkeyElement(provider, buffer.toByteArray(), backupCode);
    }

    public static SecretkeyElement createSecretkeyElement(OpenPgpProvider provider,
                                                    byte[] keys,
                                                    String backupCode)
            throws PGPException, IOException {
        byte[] encrypted = PGPainless.encryptWithPassword(keys, backupCode.toCharArray(), SymmetricKeyAlgorithm.AES_256);
        return new SecretkeyElement(Base64.encode(encrypted));
    }

    public static PGPSecretKeyRing restoreSecretKeyBackup(SecretkeyElement backup, String backupCode)
            throws InvalidBackupCodeException, IOException, PGPException {
        byte[] encrypted = Base64.decode(backup.getB64Data());

        byte[] decrypted;
        try {
            decrypted = PGPainless.decryptWithPassword(encrypted, backupCode.toCharArray());
        } catch (IOException | PGPException e) {
            throw new InvalidBackupCodeException("Could not decrypt secret key backup. Possibly wrong passphrase?", e);
        }

        return PGPainless.readKeyRing().secretKeyRing(decrypted);
    }
}
