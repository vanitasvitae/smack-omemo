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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

public interface OxMessageListener {

    /**
     * This method gets invoked, whenever an OX-IM encrypted message gets received.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0374.html">
     *     XEP-0374: OpenPGP for XMPP: Instant Messaging (OX-IM)</a>
     *
     * @param contact {@link OpenPgpContact} which sent the message.
     * @param originalMessage the received message that is carrying the encrypted {@link OpenPgpElement}.
     * @param decryptedPayload decrypted {@link SigncryptElement} which is carrying the payload.
     */
    void newIncomingOxMessage(OpenPgpContact contact,
                              Message originalMessage,
                              SigncryptElement decryptedPayload);
}
