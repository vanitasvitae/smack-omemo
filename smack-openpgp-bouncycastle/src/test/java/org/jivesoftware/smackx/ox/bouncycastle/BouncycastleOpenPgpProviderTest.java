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

import static junit.framework.TestCase.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.security.Security;
import java.util.Collections;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class BouncycastleOpenPgpProviderTest extends SmackTestSuite {

    @Test
    public void encryptAndSign_decryptAndVerifyElementTest() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Create providers for alice and the cat
        BareJid alice = JidCreate.bareFrom("alice@wonderland.lit");
        BareJid cheshire = JidCreate.bareFrom("cheshire@wonderland.lit");
        BouncycastleOpenPgpProvider aliceProvider = new BouncycastleOpenPgpProvider(alice);
        BouncycastleOpenPgpProvider cheshireProvider = new BouncycastleOpenPgpProvider(cheshire);

        // dry exchange keys
        PubkeyElement aliceKeys = aliceProvider.createPubkeyElement();
        PubkeyElement cheshireKeys = cheshireProvider.createPubkeyElement();
        aliceProvider.processRecipientsPublicKey(cheshire, cheshireKeys);
        cheshireProvider.processRecipientsPublicKey(alice, aliceKeys);

        SigncryptElement signcryptElement = new SigncryptElement(
                Collections.<Jid>singleton(cheshire),
                Collections.<ExtensionElement>singletonList(
                        new Message.Body("en", "How do you know I’m mad?")));
        OpenPgpElement encrypted = aliceProvider.signAndEncrypt(
                signcryptElement.toInputStream(),
                Collections.singleton(cheshire));

        OpenPgpMessage decrypted = cheshireProvider.decryptAndVerify(encrypted, alice);
        OpenPgpContentElement content = decrypted.getOpenPgpContentElement();
        assertTrue(content instanceof SigncryptElement);

        assertXMLEqual(signcryptElement.toXML(null).toString(), content.toXML(null).toString());
    }
}
