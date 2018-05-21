/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.ox;

import java.io.InputStream;
import java.util.Set;

import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;

import org.jxmpp.jid.BareJid;

public interface OpenPgpProvider {

    OpenPgpMessage decryptAndVerify(OpenPgpElement element, BareJid sender) throws Exception;

    OpenPgpElement signAndEncrypt(InputStream inputStream, Set<BareJid> recipients) throws Exception;

    OpenPgpElement sign(InputStream inputStream) throws Exception;

    OpenPgpElement encrypt(InputStream inputStream, Set<BareJid> recipients) throws Exception;

    PubkeyElement createPubkeyElement() throws CorruptedOpenPgpKeyException;

    void processPubkeyElement(PubkeyElement element, BareJid from) throws CorruptedOpenPgpKeyException;

    void processPublicKeysListElement(PublicKeysListElement listElement, BareJid from) throws Exception;

    /**
     * Return the OpenPGP v4-fingerprint of our key in hexadecimal upper case.
     *
     * @return fingerprint
     * @throws CorruptedOpenPgpKeyException if for some reason the fingerprint cannot be derived from the key pair.
     */
    String getFingerprint() throws CorruptedOpenPgpKeyException;
}
