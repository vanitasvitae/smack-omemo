package org.jivesoftware.smackx.ox.v2.store.abstr;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.v2.store.definition.OpenPgpTrustStore;

import org.jxmpp.jid.BareJid;

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
        if (trust == null) {
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
