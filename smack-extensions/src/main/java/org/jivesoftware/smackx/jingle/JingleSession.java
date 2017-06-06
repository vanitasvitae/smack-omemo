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
package org.jivesoftware.smackx.jingle;

import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleError;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jxmpp.jid.FullJid;

/**
 * JingleSession.
 */
public abstract class JingleSession {

    public enum State {
        fresh,
        pending,
        accepted,
        terminated,
    }

    protected final XMPPConnection connection;
    protected final FullJid initiator;
    protected final FullJid responder;
    protected final FullJid ourJid;
    protected final FullJid remote;
    protected final String sid;
    protected State sessionState;
    protected List<JingleContent> contents;

    public JingleSession(XMPPConnection connection, FullJid remote, FullJid initiator, FullJid responder, String sid) {
        this.connection = connection;
        this.ourJid = connection.getUser();
        this.remote = remote;
        this.initiator = initiator;
        this.responder = responder;
        this.sid = sid;
        this.sessionState = State.fresh;
    }

    public JingleSession(XMPPConnection connection, FullJid remote, FullJid initiator, FullJid responder) {
        this(connection, remote, initiator, responder, StringUtils.randomString(24));
    }

    public JingleSession(XMPPConnection connection, Jingle initiate) {
        if (initiate.getAction() != JingleAction.session_initiate) {
            throw new AssertionError("Session cannot be created without session-initiate");
        }
        this.connection = connection;
        this.ourJid = connection.getUser();
        this.remote = initiate.getFrom().asFullJidIfPossible();
        this.initiator = initiate.getInitiator();
        this.responder = initiate.getResponder();
        this.sid = initiate.getSid();
        this.sessionState = State.fresh;
    }

    public FullJid getInitiator() {
        return initiator;
    }

    public FullJid getResponder() {
        return responder;
    }

    public FullJid getRemote() {
        return remote;
    }

    public FullJid getOurJid() {
        return ourJid;
    }

    public String getSid() {
        return sid;
    }

    public State getSessionState() {
        return sessionState;
    }

    public IQ handleRequest(Jingle jingle) {
        switch (jingle.getAction()) {
            case session_initiate:
                // Did we already get another session-initiate?
                if (sessionState != State.fresh) {
                    return outOfOrder(jingle);
                }

                //Keep local copy of contents
                contents = jingle.getContents();

                onSessionInitiate(jingle);

                return IQ.createResultIQ(jingle);

            case session_accept:
                if (sessionState != State.pending) {
                    return outOfOrder(jingle);
                }

                onAccept(jingle);

                return IQ.createResultIQ(jingle);

            case session_terminate:

                onTerminate(jingle);

                return IQ.createResultIQ(jingle);

            case content_add:
                //TODO: Inform listeners
                return IQ.createResultIQ(jingle);

            default: return IQ.createResultIQ(jingle);
        }
    }

    public abstract void onSessionInitiate(Jingle jingle);

    public abstract void onAccept(Jingle jingle);

    public abstract void onTerminate(Jingle jingle);

    public IQ initiate(List<JingleContent> contents) {
        Jingle.Builder b = Jingle.getBuilder();
        b.setInitiator(initiator)
                .setAction(JingleAction.session_initiate)
                .setSessionId(sid);
        for (JingleContent c : contents) {
            b.addJingleContent(c);
        }

        Jingle j = b.build();
        j.setTo(remote);
        j.setFrom(ourJid);
        this.sessionState = State.pending;
        return j;
    }

    public IQ accept(Jingle jingle) {
        Jingle.Builder b = Jingle.getBuilder();
        b.setResponder(ourJid)
                .setAction(JingleAction.session_accept)
                .setSessionId(sid);
        for (JingleContent c : jingle.getContents()) {
            b.addJingleContent(c);
        }

        Jingle j = b.build();
        j.setTo(remote);
        j.setFrom(ourJid);
        this.sessionState = State.accepted;
        return j;
    }

    public IQ terminate(JingleReason.Reason reason) {
        Jingle.Builder b = Jingle.getBuilder();
        b.setAction(JingleAction.session_terminate)
                .setSessionId(sid)
                .setReason(reason);
        Jingle j = b.build();
        j.setTo(remote);
        j.setFrom(ourJid);
        this.sessionState = State.terminated;
        return b.build();
    }

    public IQ terminateFormally() {
        return terminate(JingleReason.Reason.decline);
    }

    //TODO Fix
    public IQ terminateAlternativeSession(String alternative) {
        Jingle.Builder b = Jingle.getBuilder();
        b.setAction(JingleAction.session_terminate)
                .setSessionId(sid)
                .setReason(JingleReason.Reason.alternative_session); //Set alt. sessionId
        Jingle j = b.build();
        j.setTo(remote);
        j.setFrom(ourJid);
        this.sessionState = State.terminated;
        return j;
    }

    public IQ terminateSuccessfully() {
        return terminate(JingleReason.Reason.success);
    }

    public IQ terminateBusy() {
        return terminate(JingleReason.Reason.busy);
    }

    public IQ terminateUnsupportedTransports() {
        return terminate(JingleReason.Reason.unsupported_transports);
    }

    public IQ terminateFailedTransport() {
        return terminate(JingleReason.Reason.failed_transport);
    }

    public IQ terminateUnsupportedApplications() {
        return terminate(JingleReason.Reason.unsupported_applications);
    }

    public IQ terminateFailedApplication() {
        return terminate(JingleReason.Reason.failed_application);
    }

    public IQ terminateIncompatibleParameters() {
        return terminate(JingleReason.Reason.incompatible_parameters);
    }

    public IQ unknownInitiator(Jingle jingle) {
        return IQ.createErrorResponse(jingle, XMPPError.Condition.service_unavailable);
    }

    public IQ outOfOrder(Jingle jingle) {
        XMPPError.Builder b = XMPPError.getBuilder();
        b.setCondition(XMPPError.Condition.unexpected_request);
        b.addExtension(JingleError.OUT_OF_ORDER);
        return IQ.createErrorResponse(jingle, b);
    }
}
