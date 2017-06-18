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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
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

    public Jingle createSessionInitiate(FullJid recipient,
                                        String sessionId,
                                        JingleContent.Creator contentCreator,
                                        String contentName,
                                        JingleContent.Senders contentSenders,
                                        JingleContentDescription description,
                                        JingleContentTransport transport) {

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_initiate)
                .setSessionId(sessionId)
                .setInitiator(connection.getUser());

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .addTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionInitiate(FullJid recipient,
                                  String sessionId,
                                  JingleContent.Creator contentCreator,
                                  String contentName,
                                  JingleContent.Senders contentSenders,
                                  JingleContentDescription description,
                                  JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle jingle = createSessionInitiate(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionAccept(FullJid recipient,
                                      String sessionId,
                                      JingleContent.Creator contentCreator,
                                      String contentName,
                                      JingleContent.Senders contentSenders,
                                      JingleContentDescription description,
                                      JingleContentTransport transport) {

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setResponder(connection.getUser())
                .setAction(JingleAction.session_accept)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator)
                .setName(contentName)
                .setSenders(contentSenders)
                .setDescription(description)
                .addTransport(transport);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setTo(recipient);
        jingle.setFrom(connection.getUser());

        return jingle;
    }

    public IQ sendSessionAccept(FullJid recipient,
                                String sessionId,
                                JingleContent.Creator contentCreator,
                                String contentName,
                                JingleContent.Senders contentSenders,
                                JingleContentDescription description,
                                JingleContentTransport transport)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle jingle = createSessionAccept(recipient, sessionId, contentCreator, contentName, contentSenders,
                description, transport);

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

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

    public Jingle createSessionTerminate(FullJid recipient, String sessionId, JingleReason.Reason reason) {
        return createSessionTerminate(recipient, sessionId, new JingleReason(reason));
    }

    private IQ sendSessionTerminate(FullJid recipient, String sessionId, JingleReason.Reason reason)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        return sendSessionTerminate(recipient, sessionId, new JingleReason(reason));
    }

    private IQ sendSessionTerminate(FullJid recipient, String sessionId, JingleReason reason)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminate(recipient, sessionId, reason);

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateDecline(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.decline);
    }

    public IQ sendSessionTerminateDecline(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminateDecline(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateSuccess(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.success);
    }

    public IQ sendSessionTerminateSuccess(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminateSuccess(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateBusy(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.busy);
    }

    public IQ sendSessionTerminateBusy(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminateBusy(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.AlternativeSession(altSessionId));
    }

    public IQ sendSessionTerminateAlternativeSession(FullJid recipient, String sessionId, String altSessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminateAlternativeSession(recipient, sessionId, altSessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateCancel(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.cancel);
    }

    public IQ sendSessionTerminateCancel(FullJid recipient,
                                  String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {

        Jingle jingle = createSessionTerminateCancel(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                                      JingleContent.Creator contentCreator, String contentName) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionTerminateContentCancel(FullJid recipient, String sessionId,
                                  JingleContent.Creator contentCreator, String contentName)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateContentCancel(recipient, sessionId, contentCreator, contentName);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.unsupported_transports);
    }

    public IQ sendSessionTerminateUnsupportedTransports(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateUnsupportedTransports(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateFailedTransport(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.failed_transport);
    }

    public IQ sendSessionTerminateFailedTransport(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateFailedTransport(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.unsupported_applications);
    }

    public IQ sendSessionTerminateUnsupportedApplications(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateUnsupportedApplications(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateFailedApplication(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.failed_application);
    }

    public IQ sendSessionTerminateFailedApplication(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateFailedApplication(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId) {
        return createSessionTerminate(recipient, sessionId, JingleReason.Reason.incompatible_parameters);
    }

    public IQ sendSessionTerminateIncompatibleParameters(FullJid recipient, String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        Jingle jingle = createSessionTerminateIncompatibleParameters(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public Jingle createSessionPing(FullJid recipient, String sessionId) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setSessionId(sessionId)
                .setAction(JingleAction.session_info);

        Jingle jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return jingle;
    }

    public IQ sendSessionPing(FullJid recipient, String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        Jingle jingle = createSessionPing(recipient, sessionId);
        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public IQ createErrorUnknownSession(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.item_not_found)
                .addExtension(JingleError.UNKNOWN_SESSION);
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnknownSession(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnknownSession(request));
    }

    public IQ createErrorUnknownInitiator(Jingle request) {
        return IQ.createErrorResponse(request, XMPPError.Condition.service_unavailable);
    }

    public void sendErrorUnknownInitiator(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnknownInitiator(request));
    }

    public IQ createErrorUnsupportedInfo(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.feature_not_implemented)
                .addExtension(JingleError.UNSUPPORTED_INFO);

        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorUnsupportedInfo(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorUnsupportedInfo(request));
    }


    public IQ createErrorTieBreak(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.conflict)
                .addExtension(JingleError.TIE_BREAK);
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorTieBreak(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorTieBreak(request));
    }

    public IQ createErrorOutOfOrder(Jingle request) {
        XMPPError.Builder error = XMPPError.getBuilder();
        error.setCondition(XMPPError.Condition.unexpected_request)
                .addExtension(JingleError.OUT_OF_ORDER);
        return IQ.createErrorResponse(request, error);
    }

    public void sendErrorOutOfOrder(Jingle request)
            throws SmackException.NotConnectedException, InterruptedException {
        connection.sendStanza(createErrorOutOfOrder(request));
    }
}
