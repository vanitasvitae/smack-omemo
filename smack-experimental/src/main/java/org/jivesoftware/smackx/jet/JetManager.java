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
package org.jivesoftware.smackx.jet;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jft.internal.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle Encrypted Transfers (XEP-XXXX).
 */
public final class JetManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(JetManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JetManager> INSTANCES = new WeakHashMap<>();

    private static final Map<String, JingleEncryptionMethod> encryptionMethods = new HashMap<>();

    private JetManager(XMPPConnection connection) {
        super(connection);
    }

    public static JetManager getInstanceFor(XMPPConnection connection) {
        JetManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JetManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public OutgoingFileOfferController sendEncryptedFile(FullJid recipient, File file, String encryptionMethodNamespace) throws Exception {

        JingleEncryptionMethod encryptionMethod = getEncryptionMethod(encryptionMethodNamespace);
        if (encryptionMethod == null) {
            throw new IllegalStateException("No encryption method with namespace " + encryptionMethodNamespace + " registered.");
        }

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
        Cipher cipher = Cipher.getInstance(cipherMode, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        Exception ioe = null;
        byte[] fileBuf = null;

        FileInputStream fi = null;
        CipherInputStream ci = null;

        try {
            fi = new FileInputStream(file);
            ci = new CipherInputStream(fi, cipher);

            fileBuf = new byte[(int) file.length()];
            ci.read(fileBuf);

        } catch (Exception e) {
            ioe = e;

        } finally {

            if (ci != null) {
                ci.close();
            }

            if (fi != null) {
                fi.close();
            }
        }

        if (ioe != null) {
            throw ioe;
        }

        if (fileBuf == null) {
            return null;
        }

        String contentName = StringUtils.randomString(24);

        ExtensionElement encryptionExtension = encryptionMethod.encryptJingleTransfer(recipient, keyAndIv);
        JetSecurityElement securityElement = new JetSecurityElement(contentName, encryptionExtension);

        JingleOutgoingFileOffer offer = new JingleOutgoingFileOffer(file);

        JingleFileTransferChildElement fileTransferChild = JingleFileTransferChildElement.getBuilder().setFile(file).build();
        JingleFileTransferElement fileTransfer = new JingleFileTransferElement(Collections.<JingleContentDescriptionChildElement>singletonList(fileTransferChild));

        JingleContentElement content = JingleContentElement.getBuilder()
                .setCreator(JingleContentElement.Creator.initiator)
                .setName(contentName)
                //.setTransport(offer.getTransportSession().createTransport())
                .setSecurity(securityElement)
                .setDescription(fileTransfer)
                .build();

        return offer;
    }


    public void registerEncryptionMethod(String namespace, JingleEncryptionMethod method) {
        encryptionMethods.put(namespace, method);
    }

    public void unregisterEncryptionMethod(String namespace) {
        encryptionMethods.remove(namespace);
    }

    public JingleEncryptionMethod getEncryptionMethod(String namespace) {
        return encryptionMethods.get(namespace);
    }

}
