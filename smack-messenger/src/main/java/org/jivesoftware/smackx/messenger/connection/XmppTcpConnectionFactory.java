package org.jivesoftware.smackx.messenger.connection;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import org.jxmpp.stringprep.XmppStringprepException;

public class XmppTcpConnectionFactory implements ConnectionFactory {

    @Override
    public XMPPConnection createConnection(String username, String password, String serviceName) throws XmppStringprepException {
        XMPPTCPConnectionConfiguration configuration = XMPPTCPConnectionConfiguration.builder()
                .setConnectTimeout(60 * 1000)
                .setHost(serviceName)
                .setUsernameAndPassword(username, password)
                .build();
        XMPPTCPConnection connection = new XMPPTCPConnection(configuration);

        return connection;
    }
}
