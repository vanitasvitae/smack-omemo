/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet.internal;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jet.JetManager;
import org.jivesoftware.smackx.jet.JingleEncryptionMethod;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.callbacks.JingleSecurityCallback;
import org.jivesoftware.smackx.jingle.components.JingleSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Created by vanitas on 22.07.17.
 */
public class JetSecurity extends JingleSecurity<JetSecurityElement> {

    public static final String NAMESPACE_V0 = "urn:xmpp:jingle:jet:0";
    public static final String NAMESPACE = NAMESPACE_V0;

    private final String methodNamespace;

    private final Cipher cipher;

    private ExtensionElement child;

    public JetSecurity(Cipher cipher, ExtensionElement child) {
        super();
        this.cipher = cipher;
        this.child = child;
        this.methodNamespace = child.getNamespace();
    }

    public JetSecurity(String methodNamespace, XMPPConnection connection)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        this.methodNamespace = methodNamespace;

        JetManager jetManager = JetManager.getInstanceFor(connection);
        JingleEncryptionMethod encryptionMethod = jetManager.getEncryptionMethod(methodNamespace);
        if (encryptionMethod == null) {
            throw new IllegalStateException("No encryption method with namespace " + methodNamespace + " registered.");
        }

        //Create key and cipher
        int keyLength = 256;
        String keyType = "AES";
        String cipherMode = "AES/GCM/NoPadding";

        KeyGenerator keyGenerator = KeyGenerator.getInstance(keyType);
        keyGenerator.init(keyLength);
        byte[] key = keyGenerator.generateKey().getEncoded();

        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[keyLength];
        secureRandom.nextBytes(iv);

        byte[] keyAndIv = new byte[2 * keyLength];
        System.arraycopy(key, 0, keyAndIv, 0, keyLength);
        System.arraycopy(iv, 0, keyAndIv, keyLength, keyLength);

        SecretKey secretKey = new SecretKeySpec(key, keyType);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher = Cipher.getInstance(cipherMode, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
    }

    @Override
    public JetSecurityElement getElement() {
        return new JetSecurityElement(getParent().getName(), child);
    }

    @Override
    public JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element, JingleElement wrapping) {
        return null;
    }

    @Override
    public void decryptIncomingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback) {
        JetSecurityBytestreamSession securityBytestreamSession = new JetSecurityBytestreamSession(bytestreamSession, cipher);
        callback.onSecurityReady(securityBytestreamSession);
    }

    @Override
    public void encryptIncomingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback) {
        JetSecurityBytestreamSession securityBytestreamSession = new JetSecurityBytestreamSession(bytestreamSession, cipher);
        callback.onSecurityReady(securityBytestreamSession);
    }
}
