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
package org.jivesoftware.smackx.jet.component;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.ciphers.AesGcmNoPadding;
import org.jivesoftware.smackx.jet.JetManager;
import org.jivesoftware.smackx.jet.JingleEncryptionMethod;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.callbacks.JingleSecurityCallback;
import org.jivesoftware.smackx.jingle.component.JingleSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 22.07.17.
 */
public class JetSecurity extends JingleSecurity<JetSecurityElement> {
    private static final Logger LOGGER = Logger.getLogger(JetSecurity.class.getName());

    public static final String NAMESPACE_V0 = "urn:xmpp:jingle:jet:0";
    public static final String NAMESPACE = NAMESPACE_V0;

    private final String methodNamespace;

    private AesGcmNoPadding aesKey;
    private ExtensionElement child;
    private String cipherName;
    private String name;

    public JetSecurity(JetSecurityElement element) {
        super();
        this.child = element.getChild();
        this.methodNamespace = element.getMethodNamespace();
        this.name = element.getName();
        this.cipherName = element.getCipherName();
    }

    public JetSecurity(JingleEncryptionMethod method, FullJid recipient, String name, String cipherName)
            throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, InterruptedException,
            JingleEncryptionMethod.JingleEncryptionException, SmackException.NotConnectedException,
            SmackException.NoResponseException {

        this.methodNamespace = method.getNamespace();
        this.aesKey = AesGcmNoPadding.create(cipherName);
        this.child = method.encryptJingleTransfer(recipient, aesKey.getKeyAndIv());
        this.name = name;
        this.cipherName = cipherName;
    }

    public void decryptEncryptionKey(JingleEncryptionMethod method, FullJid sender)
            throws InterruptedException, JingleEncryptionMethod.JingleEncryptionException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, NoSuchProviderException, InvalidKeyException, NoSuchPaddingException {
        byte[] keyAndIv = method.decryptJingleTransfer(sender, child);
        LOGGER.log(Level.INFO, "Transported JET Key has length: " + keyAndIv.length);
        aesKey = AesGcmNoPadding.parse(cipherName, keyAndIv);
    }

    @Override
    public JetSecurityElement getElement() {
        return new JetSecurityElement(getParent().getName(), cipherName, child);
    }

    @Override
    public JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element, JingleElement wrapping) {
        return null;
    }

    @Override
    public void decryptIncomingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback) {
        if (aesKey == null) {
            throw new IllegalStateException("Encryption key has not yet been decrypted.");
        }
        JetSecurityBytestreamSession securityBytestreamSession = new JetSecurityBytestreamSession(bytestreamSession, aesKey.getCipher());
        callback.onSecurityReady(securityBytestreamSession);
    }

    @Override
    public void encryptOutgoingBytestream(BytestreamSession bytestreamSession, JingleSecurityCallback callback) {
        JetSecurityBytestreamSession securityBytestreamSession = new JetSecurityBytestreamSession(bytestreamSession, aesKey.getCipher());
        callback.onSecurityReady(securityBytestreamSession);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void prepare(XMPPConnection connection, FullJid sender) {
        if (getParent().getParent().isInitiator()) {
            return;
        }

        if (aesKey != null) {
            return;
        }

        JingleEncryptionMethod method = JetManager.getInstanceFor(connection).getEncryptionMethod(getMethodNamespace());
        if (method == null) {
            throw new AssertionError("No JingleEncryptionMethodManager found for " + getMethodNamespace());
        }
        try {
            decryptEncryptionKey(method, sender);
        } catch (InterruptedException | NoSuchPaddingException | InvalidKeyException | NoSuchProviderException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | JingleEncryptionMethod.JingleEncryptionException e) {
            LOGGER.log(Level.SEVERE, "Could not decrypt security key: " + e, e);
        }
    }

    public String getMethodNamespace() {
        return methodNamespace;
    }
}
