package org.jivesoftware.smackx.jingle_filetransfer.handler;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.JingleBytestreamManager;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.exception.UnsupportedJingleTransportException;
import org.jivesoftware.smackx.jingle_filetransfer.FileAndHashReader;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;

/**
 * Created by vanitas on 09.06.17.
 */
public class OutgoingFileTransferInitiator implements JingleSessionHandler {

    private final WeakReference<JingleFileTransferManager> manager;
    private final JingleManager.FullJidAndSessionId fullJidAndSessionId;
    private final File file;

    public OutgoingFileTransferInitiator(JingleFileTransferManager manager, JingleManager.FullJidAndSessionId fullJidAndSessionId, File file) {
        this.fullJidAndSessionId = fullJidAndSessionId;
        this.file = file;
        this.manager = new WeakReference<>(manager);
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {
        JingleBytestreamManager<?> bm;
        try {
            bm = JingleTransportManager.getInstanceFor(manager.get().getConnection())
                    .getJingleContentTransportManager(jingle);
        } catch (UnsupportedJingleTransportException e) {
            // TODO
            return null;
        }

        switch (jingle.getAction()) {
            case session_accept:
                BytestreamSession session;

                try {
                    session = bm.outgoingInitiatedSession(jingle);
                } catch (Exception e) {
                    //TODO
                    return null;
                }

                HashElement fileHash;
                byte[] buf = new byte[(int) file.length()];

                try {
                    fileHash = FileAndHashReader.readAndCalculateHash(file, buf, HashManager.ALGORITHM.SHA_256);
                    session.getOutputStream().write(buf);
                    session.close();
                } catch (IOException e) {
                    //TODO:
                    return null;
                }
                break;
            case session_terminate:
                break;
                default:
                    break;

        }
        return IQ.createResultIQ(jingle);
    }
}
