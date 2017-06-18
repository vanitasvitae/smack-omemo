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

import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;

import org.jxmpp.jid.FullJid;

/**
 * Manager for JingleFileTransfer (XEP-0234).
 */
public final class JingleFileTransferManager extends Manager implements JingleHandler {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(JingleFileTransfer.NAMESPACE_V5);
        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(JingleFileTransfer.NAMESPACE_V5, this);
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
        JingleFileTransferSessionHandler handler = JingleFileTransferSessionHandler.getInstanceFor(connection());
        JingleManager.getInstanceFor(connection()).registerJingleSessionHandler(fullJid, sid, handler);
        return handler.handleJingleSessionRequest(jingle, jingle.getSid());
    }
}
