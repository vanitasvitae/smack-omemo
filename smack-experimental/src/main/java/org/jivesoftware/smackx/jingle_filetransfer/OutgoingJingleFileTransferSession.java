package org.jivesoftware.smackx.jingle_filetransfer;

import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;

/**
 * Created by vanitas on 04.06.17.
 */
public class OutgoingJingleFileTransferSession extends JingleSession {

    private byte[] bytes;

    public OutgoingJingleFileTransferSession(Jingle jingle) {
        super(jingle.getInitiator(), jingle.getResponder(), jingle.getSid());
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
