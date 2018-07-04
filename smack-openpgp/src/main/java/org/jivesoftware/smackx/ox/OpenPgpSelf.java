package org.jivesoftware.smackx.ox;

import org.jivesoftware.smack.XMPPConnection;

import org.jxmpp.jid.BareJid;

public class OpenPgpSelf extends OpenPgpContact {

    public OpenPgpSelf(OpenPgpProvider cryptoProvider, BareJid jid, XMPPConnection connection) {
        super(cryptoProvider, jid, connection);
    }

    public OpenPgpV4Fingerprint getSigningKey() {
        return cryptoProvider.getStore().getSigningKeyPairFingerprint();
    }
}
