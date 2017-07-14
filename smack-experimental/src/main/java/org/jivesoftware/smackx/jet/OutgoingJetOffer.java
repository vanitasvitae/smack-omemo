package org.jivesoftware.smackx.jet;

import java.io.File;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle_filetransfer.OutgoingJingleFileOffer;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 14.07.17.
 */
public class OutgoingJetOffer extends OutgoingJingleFileOffer {

    public OutgoingJetOffer(XMPPConnection connection, FullJid responder, String sid) {
        super(connection, responder, sid);
    }

    public OutgoingJetOffer(XMPPConnection connection, FullJid recipient) {
        super(connection, recipient);
    }

    @Override
    public void send(File file) {

    }
}
