package org.jivesoftware.smackx.ox.exception;

import org.jxmpp.jid.BareJid;

public class MissingUserIdOnKeyException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingUserIdOnKeyException(BareJid owner, long keyId) {
        super("Key " + Long.toHexString(keyId) + " does not have a user-id of \"xmpp:" + owner.toString() + "\".");
    }
}
