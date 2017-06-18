/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.jingle;

import org.jxmpp.jid.FullJid;

public class JingleSession {

    protected final FullJid local;

    protected final FullJid remote;

    protected final Role role;

    protected final String sid;

    public JingleSession(FullJid initiator, FullJid responder, Role role, String sid) {
        if (role == Role.initiator) {
            this.local = initiator;
            this.remote = responder;
        } else {
            this.local = responder;
            this.remote = initiator;
        }
        this.sid = sid;
        this.role = role;
    }

    public FullJid getInitiator() {
        return isInitiator() ? local : remote;
    }

    public boolean isInitiator() {
        return role == Role.initiator;
    }

    public FullJid getResponder() {
        return isResponder() ? local : remote;
    }

    public boolean isResponder() {
        return role == Role.responder;
    }

    public String getSessionId() {
        return sid;
    }

    public FullJidAndSessionId getFullJidAndSessionId() {
        return new FullJidAndSessionId(remote, sid);
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + getInitiator().hashCode();
        hashCode = 31 * hashCode + getResponder().hashCode();
        hashCode = 31 * hashCode + getSessionId().hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JingleSession)) {
            return false;
        }

        JingleSession otherJingleSession = (JingleSession) other;
        return getInitiator().equals(otherJingleSession.getInitiator())
                && getResponder().equals(otherJingleSession.getResponder())
                && sid.equals(otherJingleSession.sid);
    }
}
