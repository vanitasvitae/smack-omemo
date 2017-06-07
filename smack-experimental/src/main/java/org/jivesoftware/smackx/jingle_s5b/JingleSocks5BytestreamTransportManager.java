package org.jivesoftware.smackx.jingle_s5b;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;

/**
 * Created by vanitas on 07.06.17.
 */
public class JingleSocks5BytestreamTransportManager extends Manager {

    private JingleSocks5BytestreamTransportManager(XMPPConnection connection) {
        super(connection);
    }
}
