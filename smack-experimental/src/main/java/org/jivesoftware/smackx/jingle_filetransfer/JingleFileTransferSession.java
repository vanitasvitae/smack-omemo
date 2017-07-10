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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.Role;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.handler.FileTransferHandler;

import org.jxmpp.jid.FullJid;

/**
 * Class representing a Jingle session in the context of Jingle File Transfer (XEP-0234).
 */
public abstract class JingleFileTransferSession extends JingleSession implements FileTransferHandler {

    protected final ArrayList<EndedListener> endedListeners = new ArrayList<>();
    protected final ArrayList<StartedListener> startedListeners = new ArrayList<>();

    protected boolean started, ended;

    public enum Type {
        offer,
        request,
        ;
    }

    protected final XMPPConnection connection;
    protected final JingleUtil jutil;

    protected JingleFileTransfer file;

    private final Type type;

    public JingleFileTransferSession(XMPPConnection connection, FullJid initiator, FullJid responder, Role role, String sid, Type type) {
        super(initiator, responder, role, sid);
        this.type = type;
        this.connection = connection;
        this.jutil = new JingleUtil(connection);
    }

    public Type getType() {
        return type;
    }

    public boolean isOffer() {
        return this.type == Type.offer;
    }

    public boolean isRequest() {
        return this.type == Type.request;
    }

    public boolean isSender() {
        return (isOffer() && isInitiator()) || (isRequest() && isResponder());
    }

    public boolean isReceiver() {
        return (isRequest() && isInitiator()) || (isOffer() && isResponder());
    }

    @Override
    public boolean isFinished() {
        return ended;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void addEndedListener(EndedListener listener) {
        endedListeners.add(listener);
    }

    @Override
    public void addStartedListener(StartedListener listener) {
        startedListeners.add(listener);
    }

    @Override
    public void notifyEndedListeners(JingleReason.Reason reason) {
        ended = true;
        for (EndedListener e : endedListeners) {
            e.onEnded(reason);
        }
    }

    @Override
    public void notifyStartedListeners() {
        started = true;
        for (StartedListener s : startedListeners) {
            s.onStarted();
        }
    }

    @Override
    public XMPPConnection getConnection() {
        return connection;
    }
}
