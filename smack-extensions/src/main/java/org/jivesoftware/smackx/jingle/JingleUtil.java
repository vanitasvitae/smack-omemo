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

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.jingle3.element.JingleAction;
import org.jivesoftware.smackx.jingle3.element.JingleContentElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleErrorElement;
import org.jivesoftware.smackx.jingle3.element.JingleReasonElement;

import org.jxmpp.jid.FullJid;

/**
 * Util to quickly create and send jingle stanzas.
 */
public class JingleUtil {

    private final XMPPConnection connection;

    public JingleUtil(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Initiate a Jingle session.
     * XEP-0166 Example 10.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId
     * @param content content
     * @return session-initiate stanza.
     */
    public JingleElement createSessionInitiate(FullJid recipient,
                                               String sessionId,
                                               JingleContentElement content) {
        return createSessionInitiate(recipient, sessionId, Collections.singletonList(content));
    }

    public JingleElement createSessionInitiate(FullJid recipient,
                                               String sessionId,
                                               List<JingleContentElement> contents) {

        JingleElement.Builder builder = JingleElement.getBuilder();
        builder.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(connection.getUser());

        for (JingleContentElement content : contents) {
            builder.addJingleContent(content);
        }

        JingleElement jingle = builder.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Accept a session.
     * XEP-0166 Example 17.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param content content
     * @return session-accept stanza.
     */
    public JingleElement createSessionAccept(FullJid recipient,
                                             String sessionId,
                                             JingleContentElement content) {
        return createSessionAccept(recipient, sessionId, Collections.singletonList(content));
    }

    public JingleElement createSessionAccept(FullJid recipient,
                                             String sessionId,
                                             List<JingleContentElement> contents) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setResponder(connection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        for (JingleContentElement content : contents) {
            jb.addJingleContent(content);
        }

        JingleElement jingle = jb.build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    /**
     * Cancel a single contents transfer.
     * XEP-0234 Example 10.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param contentCreator creator of the content.
     * @param contentName name of the content.
     * @return session-terminate stanza.
     */
    public JingleElement createSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                                             JingleContentElement.Creator contentCreator, String contentName) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId).setReason(JingleReasonElement.Reason.cancel);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Create a session ping stanza.
     * XEP-0166 Example 32.
     * @param recipient recipient of the stanza
     * @param sessionId id of the session
     * @return ping stanza
     */
    public JingleElement createSessionPing(FullJid recipient, String sessionId) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        JingleElement jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Acknowledge the receipt of a stanza.
     * XEP-0166 Example 5.
     * @param jingle stanza that was received
     * @return acknowledgement
     */
    public IQ createAck(JingleElement jingle) {
        return IQ.createResultIQ(jingle);
    }

    /**
     * Replace a transport with another one.
     * XEP-0260 Example 15.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport proposed transport
     * @return transport-replace stanza
     */
    public JingleElement createTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
                                                JingleContentElement.Creator contentCreator, String contentName,
                                                JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setInitiator(initiator)
                .setSessionId(sessionId)
                .setAction(JingleAction.transport_replace);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setName(contentName).setCreator(contentCreator).setTransport(transport);
        JingleElement jingle = jb.addJingleContent(cb.build()).build();

        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    /**
     * Accept a transport.
     * XEP-0260 Example 17.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport transport to accept
     * @return transport-accept stanza
     */
    public JingleElement createTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
                                               JingleContentElement.Creator contentCreator, String contentName,
                                               JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.transport_accept)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    /**
     * Reject a transport.
     * XEP-0166 §7.2.14.
     * @param recipient recipient of the stanza
     * @param initiator initiator of the session
     * @param sessionId sessionId
     * @param contentCreator creator of the content
     * @param contentName name of the content
     * @param transport transport to reject
     * @return transport-reject stanza
     */
    public JingleElement createTransportReject(FullJid recipient, FullJid initiator, String sessionId,
                                               JingleContentElement.Creator contentCreator, String contentName,
                                               JingleContentTransportElement transport) {
        JingleElement.Builder jb = JingleElement.getBuilder();
        jb.setAction(JingleAction.transport_reject)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContentElement.Builder cb = JingleContentElement.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        JingleElement jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    /*
     * ####################################################################################################
     */

    /**
     * Create an error response to a request with an unknown session id.
     * XEP-0166 Example 29.
     * @param request request with unknown sessionId.
     * @return error stanza.
     */
    public IQ createErrorUnknownSession(JingleElement request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.item_not_found)
                .addExtension(JingleErrorElement.UNKNOWN_SESSION);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request coming from a unknown initiator.
     * XEP-0166 Example 12.
     * @param request request from unknown initiator.
     * @return error stanza.
     */
    public IQ createErrorUnknownInitiator(JingleElement request) {
        XMPPError.Builder b = XMPPError.getBuilder().setType(XMPPError.Type.CANCEL).setCondition(XMPPError.Condition.service_unavailable);
        return IQ.createErrorResponse(request, b);
    }

    /**
     * Create an error response to a request with an unsupported info.
     * XEP-0166 Example 31.
     * @param request request with unsupported info.
     * @return error stanza.
     */
    public IQ createErrorUnsupportedInfo(JingleElement request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.feature_not_implemented)
                .addExtension(JingleErrorElement.UNSUPPORTED_INFO).setType(XMPPError.Type.MODIFY);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a tie-breaking request.
     * XEP-0166 Example 34.
     * @param request tie-breaking request
     * @return error stanza
     */
    public IQ createErrorTieBreak(JingleElement request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.conflict)
                .addExtension(JingleErrorElement.TIE_BREAK);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request that was out of order.
     * TODO: Find example.
     * @param request request out of order.
     * @return error stanza.
     */
    public IQ createErrorOutOfOrder(JingleElement request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.unexpected_request)
                .addExtension(JingleErrorElement.OUT_OF_ORDER);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a malformed request.
     * XEP-0166 Ex. 16
     * @param request malformed request
     * @return error stanza.
     */
    public IQ createErrorMalformedRequest(JingleElement request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setType(XMPPError.Type.CANCEL);
        error.setCondition(XMPPError.Condition.bad_request);
        return IQ.createErrorResponse(request, error);
    }
}
