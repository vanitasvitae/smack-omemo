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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jxmpp.jid.Jid;

// TODO: Is this class still required? If not, then remove it.
public class JingleSession {

    public enum State {
        pending,
        active,
        ;
    }

    private final Jid initiator;

    private final Jid responder;

    private final String sid;

    private State state = State.pending;

    public JingleSession(Jid initiator, Jid responder, String sid) {
        this.initiator = initiator;
        this.responder = responder;
        this.sid = sid;
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + initiator.hashCode();
        hashCode = 31 * hashCode + responder.hashCode();
        hashCode = 31 * hashCode + sid.hashCode();
        return hashCode;
    }

    public String getSid() {
        return sid;
    }

    public Jid getInitiator() {
        return initiator;
    }

    public Jid getResponder() {
        return responder;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JingleSession)) {
            return false;
        }

        JingleSession otherJingleSession = (JingleSession) other;
        return initiator.equals(otherJingleSession.initiator) && responder.equals(otherJingleSession.responder)
                        && sid.equals(otherJingleSession.sid);
    }

    IQ terminateSuccessfully() {
        Jingle.Builder builder = Jingle.getBuilder();
        builder.setAction(JingleAction.session_terminate);
        builder.setSessionId(getSid());
        builder.setReason(JingleReason.Reason.success);

        return builder.build();
    }
}
