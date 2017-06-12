/**
 *
 * Copyright © 2017 Paul Schaub
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

import java.io.File;
import java.util.ArrayList;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.callback.JingleFileTransferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingJingleFileTransferListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_s5b.JingleS5BTransportManager;
import org.jxmpp.jid.FullJid;

/**
 * Manager for JingleFileTransfer.
 */
public final class JingleFileTransferManager extends Manager implements JingleHandler {
    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();

    private final ArrayList<IncomingJingleFileTransferListener> incomingJFTListeners = new ArrayList<>();

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentDescriptionProvider(NAMESPACE_V5, new JingleFileTransferContentDescriptionProvider());
        JingleManager.getInstanceFor(connection).registerDescriptionHandler(NAMESPACE_V5, this);
        //JingleIBBTransportManager.getInstanceFor(connection);
        JingleS5BTransportManager.getInstanceFor(connection);
    }

    public static JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void addIncomingJingleFileTransferListener(IncomingJingleFileTransferListener listener) {
        incomingJFTListeners.add(listener);
    }

    void notifyIncomingJingleFileTransferListeners(JingleFileTransferChild file, JingleFileTransferCallback callback) {
        for (IncomingJingleFileTransferListener l : incomingJFTListeners) {
            l.onIncomingJingleFileTransfer(file, callback);
        }
    }

    public void addJingleSession(JingleFileTransferSession session) {
        JingleManager.FullJidAndSessionId fns = session.getFullJidAndSessionId();
        JingleManager.getInstanceFor(connection())
                .registerJingleSessionHandler(fns.getFullJid(), fns.getSessionId(), session);
    }

    public void removeJingleSession(JingleFileTransferSession session) {
        JingleManager.FullJidAndSessionId fns = session.getFullJidAndSessionId();
        JingleManager.getInstanceFor(connection()).unregisterJingleSession(
                fns.getFullJid(), fns.getSessionId());
    }

    @Override
    public IQ handleJingleSessionInitiate(Jingle jingle) {
        JingleFileTransferSession fresh = new JingleFileTransferSession(connection(), jingle);
        addJingleSession(fresh);
        return fresh.handleJingleSessionRequest(jingle);
    }

    public void sendFile(FullJid recipient, File file) {
        JingleFileTransferSession fresh = new JingleFileTransferSession(connection(), recipient, file);
        addJingleSession(fresh);
    }
}
