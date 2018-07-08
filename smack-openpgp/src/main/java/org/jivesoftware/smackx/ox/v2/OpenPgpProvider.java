/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.v2;

import java.io.IOException;
import java.util.Collection;

import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;

import org.bouncycastle.openpgp.PGPException;

public interface OpenPgpProvider {

    OpenPgpElement signAndEncrypt(SigncryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients) throws IOException, PGPException;

    OpenPgpElement sign(SignElement element, OpenPgpSelf self) throws IOException, PGPException;

    OpenPgpElement encrypt(CryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients) throws IOException, PGPException;

    OpenPgpMessage decryptAndOrVerify(OpenPgpElement element, OpenPgpSelf self, OpenPgpContact sender) throws IOException, PGPException;
}
