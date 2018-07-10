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
package org.jivesoftware.smackx.ox.store.abstr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;

import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;

public abstract class AbstractOpenPgpTrustStore implements OpenPgpTrustStore {

    private final Map<BareJid, Map<OpenPgpV4Fingerprint, Trust>> trustCache = new HashMap<>();

    protected abstract Trust readTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException;

    protected abstract void writeTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) throws IOException;

    @Override
    public Trust getTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException {
        Trust trust;
        Map<OpenPgpV4Fingerprint, Trust> trustMap = trustCache.get(owner);

        if (trustMap != null) {
            trust = trustMap.get(fingerprint);
            if (trust != null) {
                return trust;
            }
        } else {
            trustMap = new HashMap<>();
            trustCache.put(owner, trustMap);
        }

        trust = readTrust(owner, fingerprint);
        trustMap.put(fingerprint, trust);

        return trust;
    }

    @Override
    public void setTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) throws IOException {
        Map<OpenPgpV4Fingerprint, Trust> trustMap = trustCache.get(owner);
        if (trustMap == null) {
            trustMap = new HashMap<>();
            trustCache.put(owner, trustMap);
        }

        if (trustMap.get(fingerprint) == trust) {
            return;
        }

        trustMap.put(fingerprint, trust);
        writeTrust(owner, fingerprint, trust);
    }
}
