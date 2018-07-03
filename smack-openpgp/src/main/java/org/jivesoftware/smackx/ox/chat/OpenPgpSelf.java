package org.jivesoftware.smackx.ox.chat;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.ox.OpenPgpProvider;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import org.jxmpp.jid.BareJid;

public class OpenPgpSelf extends OpenPgpContact {

    public OpenPgpSelf(OpenPgpProvider cryptoProvider, BareJid jid, XMPPConnection connection) {
        super(cryptoProvider, jid, connection);
    }

    public OpenPgpV4Fingerprint getSigningKey() {
        return cryptoProvider.getStore().getSigningKeyPairFingerprint();
    }
}
