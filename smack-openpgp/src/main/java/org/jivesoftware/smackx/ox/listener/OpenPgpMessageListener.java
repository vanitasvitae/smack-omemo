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
package org.jivesoftware.smackx.ox.listener;

import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

import org.jxmpp.jid.BareJid;

interface OpenPgpMessageListener {

    /**
     * This method gets called whenever we received and successfully decrypted/verified an encrypted, signed message.
     *
     * @param from sender/signer of the message.
     * @param signcryptElement decrypted and verified {@link SigncryptElement}.
     */
    void signcryptElementReceived(BareJid from, SigncryptElement signcryptElement);

    /**
     * This method gets called whenever we received and successfully verified a signed message.
     *
     * @param from sender/signer of the message.
     * @param signElement verified {@link SignElement}.
     */
    void signElementReceived(BareJid from, SignElement signElement);

    /**
     * This method gets called whenever we received and successfully decrypted an encrypted message.
     *
     * @param from sender of the message.
     * @param cryptElement decrypted {@link CryptElement}.
     */
    void cryptElementReceived(BareJid from, CryptElement cryptElement);
}
