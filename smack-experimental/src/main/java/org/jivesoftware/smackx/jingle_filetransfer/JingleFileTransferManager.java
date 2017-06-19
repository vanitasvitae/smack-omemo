/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle_filetransfer;

import java.util.ArrayList;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.listener.JingleFileTransferOfferListener;

import org.jxmpp.jid.FullJid;

/**
 * Manager for JingleFileTransfer (XEP-0234).
 */
public final class JingleFileTransferManager extends Manager implements JingleHandler {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();
    private final JingleUtil jutil;
    private final ArrayList<JingleFileTransferOfferListener> jingleFileTransferOfferListeners = new ArrayList<>();

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(JingleFileTransfer.NAMESPACE_V5);
        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(JingleFileTransfer.NAMESPACE_V5, this);
        jutil = new JingleUtil(connection);
    }

    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        FullJid fullJid = jingle.getFrom().asFullJidOrThrow();
        String sid = jingle.getSid();

        //Get handler
        JingleFileTransferSession handler;
        try {
            handler = createSessionHandler(jingle);
        } catch (IllegalArgumentException malformed) {
            // If senders is neither initiator, nor responder, consider session malformed.
            // See XEP-0166 §6.3 Example 16 and XEP-0234 §4.1
            return jutil.createErrorMalformedRequest(jingle);
        }

        JingleManager.getInstanceFor(connection()).registerJingleSessionHandler(fullJid, sid, handler);
        return handler.handleJingleSessionRequest(jingle);
    }

    /**
     * Create a session handler (FileOffer or FileRequest) for the request.
     * @param request
     * @return
     */
    private JingleFileTransferSession createSessionHandler(Jingle request) {
        if (request.getAction() != JingleAction.session_initiate) {
            throw new IllegalArgumentException("Requests action MUST be session-initiate.");
        }

        JingleContent content = request.getContents().get(0);
        //File Offer
        if (content.getSenders() == JingleContent.Senders.initiator) {
            return new IncomingJingleFileOffer(connection(), request);
        } //File Request
        else if (content.getSenders() == JingleContent.Senders.responder) {
            return JingleFileRequest.createIncomingFileRequest(connection(), request);
        } //Malformed Request
        else {
            throw new IllegalArgumentException("Requests content.senders MUST be either responder or initiator.");
        }
    }

    public void notifyIncomingFileOffer(Jingle initiate, IncomingFileOfferCallback callback) {
        for (JingleFileTransferOfferListener l : jingleFileTransferOfferListeners) {
            l.onFileOffer(initiate, callback);
        }
    }

    public void addJingleFileTransferOfferListener(JingleFileTransferOfferListener listener) {
        jingleFileTransferOfferListeners.add(listener);
    }

    public void remove(JingleFileTransferOfferListener listener) {
        jingleFileTransferOfferListeners.remove(listener);
    }
}
