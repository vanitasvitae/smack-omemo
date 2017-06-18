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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.element.Jingle;

/**
 * Handler for JingleFileTransfer sessions.
 */
public final class JingleFileTransferSessionHandler implements JingleSessionHandler {

    private static final WeakHashMap<XMPPConnection, JingleFileTransferSessionHandler> INSTANCES = new WeakHashMap<>();

    private final XMPPConnection connection;
    private final JingleUtil jutil;

    private JingleFileTransferSessionHandler(XMPPConnection connection) {
        this.connection = connection;
        jutil = new JingleUtil(connection);
    }

    public static JingleFileTransferSessionHandler getInstanceFor(XMPPConnection connection) {
        JingleFileTransferSessionHandler handler = INSTANCES.get(connection);
        if (handler == null) {
            handler = new JingleFileTransferSessionHandler(connection);
            INSTANCES.put(connection, handler);
        }
        return handler;
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle, String sessionId) {

        return null;
    }
}
