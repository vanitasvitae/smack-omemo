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
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleError;
import org.jivesoftware.smackx.jingle.element.JingleReason;

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
    public Jingle createSessionInitiate(FullJid recipient,
                                        String sessionId,
                                        JingleContent content) {
        return createSessionInitiate(recipient, sessionId, Collections.singletonList(content));
    }

    public Jingle createSessionInitiate(FullJid recipient,
                                        String sessionId,
                                        List<JingleContent> contents) {

        Jingle.Builder builder = Jingle.getBuilder();
        builder.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(connection.getUser());

        for (JingleContent content : contents) {
            builder.addJingleContent(content);
        }

        Jingle jingle = builder.build();
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
    public Jingle createSessionAccept(FullJid recipient,
                                      String sessionId,
                                      JingleContent content) {
        return createSessionAccept(recipient, sessionId, Collections.singletonList(content));
    }

    public Jingle createSessionAccept(FullJid recipient,
                                      String sessionId,
                                      List<JingleContent> contents) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setResponder(connection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        for (JingleContent content : contents) {
            jb.addJingleContent(content);
        }

        Jingle jingle = jb.build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    /**
     * Create a session-terminate stanza.
     * XEP-0166 §6.7.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param reason reason of termination.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminate(FullJid recipient, String sessionId, JingleReason reason) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId)
                .setReason(reason);

        Jingle jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Create a session-terminate stanza.
     * XEP-0166 §6.7.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param reason reason of termination.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminate(FullJid recipient, String sessionId, JingleReason.Reason reason) {
        return createSessionTerminate(recipient, sessionId, new JingleReason(reason));
    }

    /**
     * Terminate the session by declining.
     * XEP-0166 Example 21.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateDecline(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.decline);
    }

    /**
     * Terminate the session due to success.
     * XEP-0166 Example 19.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateSuccess(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.success);
    }

    /**
     * Terminate the session due to being busy.
     * XEP-0166 Example 20.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateBusy(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.busy);
    }

    /**
     * Terminate the session due to the existence of an alternative session.
     * XEP-0166 Example 22.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @param altSessionId id of the alternative session.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.AlternativeSession(altSessionId));
    }

    /**
     * Cancel all active transfers of the session.
     * XEP-0234 Example 9.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateCancel(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.cancel);
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
    public Jingle createSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                                      JingleContent.Creator contentCreator, String contentName) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId).setReason(JingleReason.Reason.cancel);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    /**
     * Terminate the session due to unsupported transport methods.
     * XEP-0166 Example 23.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.unsupported_transports);
    }

    /**
     * Terminate the session due to failed transports.
     * XEP-0166 Example 24.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateFailedTransport(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.failed_transport);
    }

    /**
     * Terminate the session due to unsupported applications.
     * XEP-0166 Example 25.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.unsupported_applications);
    }

    /**
     * Terminate the session due to failed application.
     * XEP-0166 Example 26.
     * @param recipient recipient of the stanza.
     * @param sessionId sessionId.
     * @return session-terminate stanza.
     */
    public Jingle createSessionTerminateFailedApplication(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.failed_application);
    }

    /**
     * Terminate session due to incompatible parameters.
     * XEP-0166 Example 27.
     * @param recipient recipient of the stanza
     * @param sessionId sessionId
     * @return session-terminate stanza
     */
    public Jingle createSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.incompatible_parameters);
    }

    public IQ sendContentRejectFileNotAvailable(FullJid recipient, String sessionId, JingleContentDescription description) {
        return null; //TODO Later
    }

    /**
     * Create a session ping stanza.
     * XEP-0166 Example 32.
     * @param recipient recipient of the stanza
     * @param sessionId id of the session
     * @return ping stanza
     */
    public Jingle createSessionPing(FullJid recipient, String sessionId) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        Jingle jingle = jb.build();
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
    public IQ createAck(Jingle jingle) {
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
    public Jingle createTransportReplace(FullJid recipient, FullJid initiator, String sessionId,
                                         JingleContent.Creator contentCreator, String contentName,
                                         JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setInitiator(initiator)
                .setSessionId(sessionId)
                .setAction(JingleAction.transport_replace);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setName(contentName).setCreator(contentCreator).setTransport(transport);
        Jingle jingle = jb.addJingleContent(cb.build()).build();

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
    public Jingle createTransportAccept(FullJid recipient, FullJid initiator, String sessionId,
                                        JingleContent.Creator contentCreator, String contentName,
                                        JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.transport_accept)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
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
    public Jingle createTransportReject(FullJid recipient, FullJid initiator, String sessionId,
                                        JingleContent.Creator contentCreator, String contentName,
                                        JingleContentTransport transport) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.transport_reject)
                .setInitiator(initiator)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName).setTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
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
    public IQ createErrorUnknownSession(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.item_not_found)
                .addExtension(JingleError.UNKNOWN_SESSION);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request coming from a unknown initiator.
     * XEP-0166 Example 12.
     * @param request request from unknown initiator.
     * @return error stanza.
     */
    public IQ createErrorUnknownInitiator(Jingle request) {
        XMPPError.Builder b = XMPPError.getBuilder().setType(XMPPError.Type.CANCEL).setCondition(XMPPError.Condition.service_unavailable);
        return IQ.createErrorResponse(request, b);
    }

    /**
     * Create an error response to a request with an unsupported info.
     * XEP-0166 Example 31.
     * @param request request with unsupported info.
     * @return error stanza.
     */
    public IQ createErrorUnsupportedInfo(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.feature_not_implemented)
                .addExtension(JingleError.UNSUPPORTED_INFO).setType(XMPPError.Type.MODIFY);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a tie-breaking request.
     * XEP-0166 Example 34.
     * @param request tie-breaking request
     * @return error stanza
     */
    public IQ createErrorTieBreak(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.conflict)
                .addExtension(JingleError.TIE_BREAK);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a request that was out of order.
     * TODO: Find example.
     * @param request request out of order.
     * @return error stanza.
     */
    public IQ createErrorOutOfOrder(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.unexpected_request)
                .addExtension(JingleError.OUT_OF_ORDER);
        return IQ.createErrorResponse(request, error);
    }

    /**
     * Create an error response to a malformed request.
     * XEP-0166 Ex. 16
     * @param request malformed request
     * @return error stanza.
     */
    public IQ createErrorMalformedRequest(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setType(XMPPError.Type.CANCEL);
        error.setCondition(XMPPError.Condition.bad_request);
        return IQ.createErrorResponse(request, error);
    }
}
