package org.jivesoftware.smackx.messenger.connection;

import org.jivesoftware.smack.XMPPConnection;

import org.jxmpp.stringprep.XmppStringprepException;

public interface ConnectionFactory {

    XMPPConnection createConnection(String username, String password, String serviceName) throws XmppStringprepException;
}
