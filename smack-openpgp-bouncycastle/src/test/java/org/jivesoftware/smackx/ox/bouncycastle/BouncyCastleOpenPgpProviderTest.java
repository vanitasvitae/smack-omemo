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

import java.security.Security;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Ignore;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;

public class BouncyCastleOpenPgpProviderTest extends SmackTestSuite {

    @Ignore
    public void encryptAndSign_decryptAndVerifyElementTest() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Create providers for alice and the cat
        BareJid alice = JidCreate.bareFrom("alice@wonderland.lit");
        BareJid cheshire = JidCreate.bareFrom("cheshire@wonderland.lit");
        BouncyCastleOpenPgpProvider aliceProvider = new BouncyCastleOpenPgpProvider(alice);
        BouncyCastleOpenPgpProvider cheshireProvider = new BouncyCastleOpenPgpProvider(cheshire);

        aliceProvider.createAndUseKey();
        cheshireProvider.createAndUseKey();

        // dry exchange keys
        /*
        PubkeyElement aliceKeys = aliceProvider.createPubkeyElement();
        PubkeyElement cheshireKeys = cheshireProvider.createPubkeyElement();
        aliceProvider.storePublicKey(cheshireKeys, cheshire);
        cheshireProvider.storePublicKey(aliceKeys, alice);

        // Create signed and encrypted message from alice to the cheshire cat
        SigncryptElement signcryptElement = new SigncryptElement(
                Collections.<Jid>singleton(cheshire),
                Collections.<ExtensionElement>singletonList(
                        new Message.Body("en", "How do you know I’m mad?")));
        OpenPgpElement encrypted = aliceProvider.signAndEncrypt(
                signcryptElement,
                Collections.singleton(cheshire));

        // Decrypt the message as the cheshire cat
        OpenPgpMessage decrypted = cheshireProvider.decryptAndVerify(encrypted, alice);
        OpenPgpContentElement content = decrypted.getOpenPgpContentElement();

        assertTrue(content instanceof SigncryptElement);
        assertXMLEqual(signcryptElement.toXML(null).toString(), content.toXML(null).toString());
        */
    }
}
