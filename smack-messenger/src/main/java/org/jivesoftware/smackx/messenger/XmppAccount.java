package org.jivesoftware.smackx.messenger;

import java.io.IOException;
import java.util.UUID;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

public class XmppAccount {

    private final UUID accountId;
    private final XMPPConnection connection;

    public XmppAccount(UUID accountId, XMPPConnection connection) {
        this.connection = connection;
        this.accountId = accountId;
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void login() throws InterruptedException, XMPPException, SmackException, IOException {
        ((AbstractXMPPConnection) getConnection()).connect().login();
    }

    public boolean isLoggedIn() {
        return getConnection().isAuthenticated();
    }
}
