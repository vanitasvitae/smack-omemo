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
package org.jivesoftware.smackx.jingle.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.util.Role;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.exception.UnsupportedDescriptionException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedSecurityException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedTransportException;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;

import org.jxmpp.jid.FullJid;

/**
 * Class that represents a Jingle session.
 */
public class JingleSession {
    private static final Logger LOGGER = Logger.getLogger(JingleSession.class.getName());

    private final ConcurrentHashMap<String, JingleContent> contents = new ConcurrentHashMap<>();
    private final JingleManager jingleManager;

    private final FullJid initiator, responder;
    private final Role role;
    private final String sessionId;

    private final Map<JingleContent, PendingJingleAction> pendingJingleActions =
            Collections.synchronizedMap(new HashMap<JingleContent, PendingJingleAction>());

    public JingleSession(JingleManager manager, FullJid initiator, FullJid responder, Role role, String sessionId) {
        this.jingleManager = manager;
        this.initiator = initiator;
        this.responder = responder;
        this.role = role;
        this.sessionId = sessionId;
    }

    public void addContent(JingleContent content) {
        contents.put(content.getName(), content);
        content.setParent(this);
    }

    public void addContent(JingleContentElement content)
            throws UnsupportedSecurityException, UnsupportedTransportException, UnsupportedDescriptionException {
        addContent(JingleContent.fromElement(content));
    }

    public ConcurrentHashMap<String, JingleContent> getContents() {
        return contents;
    }

    public JingleContent getContent(String name) {
        return contents.get(name);
    }

    public JingleContent getSoleContentOrThrow() {
        if (contents.isEmpty()) {
            return null;
        }

        if (contents.size() > 1) {
            throw new IllegalStateException();
        }

        return contents.values().iterator().next();
    }

    public static JingleSession fromSessionInitiate(JingleManager manager, JingleElement initiate)
            throws UnsupportedSecurityException, UnsupportedDescriptionException, UnsupportedTransportException {
        if (initiate.getAction() != JingleAction.session_initiate) {
            throw new IllegalArgumentException("Jingle-Action MUST be 'session-initiate'.");
        }

        JingleSession session = new JingleSession(manager, initiate.getInitiator(), initiate.getResponder(), Role.responder, initiate.getSid());
        List<JingleContentElement> initiateContents = initiate.getContents();

        for (JingleContentElement content : initiateContents) {
            session.addContent(content);
        }

        return session;
    }

    public JingleElement createSessionInitiate() {
        if (role != Role.initiator) {
            throw new IllegalStateException("Sessions role is not initiator.");
        }

        List<JingleContentElement> contentElements = new ArrayList<>();
        for (JingleContent c : contents.values()) {
            contentElements.add(c.getElement());
        }

        return JingleElement.createSessionInitiate(getInitiator(), getResponder(), getSessionId(), contentElements);
    }

    public IQ handleJingleRequest(JingleElement request) {
        switch (request.getAction()) {
            case content_accept:
                return handleContentAccept(request);
            case content_add:
                return handleContentAdd(request);
            case content_modify:
                return handleContentModify(request);
            case content_reject:
                return handleContentReject(request);
            case content_remove:
                return handleContentRemove(request);
            case description_info:
                return handleDescriptionInfo(request);
            case session_info:
                return handleSessionInfo(request);
            case security_info:
                return handleSecurityInfo(request);
            case session_accept:
                return handleSessionAccept(request);
            case transport_accept:
                return handleTransportAccept(request);
            case transport_info:
                return handleTransportInfo(request);
            case session_initiate:
                return handleSessionInitiate(request);
            case transport_reject:
                return handleTransportReject(request);
            case session_terminate:
                return handleSessionTerminate(request);
            case transport_replace:
                return handleTransportReplace(request);
            default:
                throw new AssertionError("Unknown Jingle Action enum! " + request.getAction());
        }
    }

    private IQ handleTransportReplace(JingleElement request) {
        List<JingleContentElement> affectedContents = request.getContents();
        List<JingleElement> responses = new ArrayList<>();

        for (JingleContentElement affected : affectedContents) {
            JingleContent content = contents.get(affected.getName());
            JingleContentTransportElement newTransport = affected.getTransport();
            Set<String> blacklist = content.getTransportBlacklist();

            // Proposed transport method might already be on the blacklist (eg. because of previous failures)
            if (blacklist.contains(newTransport.getNamespace())) {
                responses.add(JingleElement.createTransportReject(getInitiator(), getPeer(), getSessionId(),
                        content.getCreator(), content.getName(), newTransport));
                continue;
            }

            JingleTransportAdapter<?> transportAdapter = JingleManager.getJingleTransportAdapter(
                    newTransport.getNamespace());
            // This might be an unknown transport.
            if (transportAdapter == null) {
                responses.add(JingleElement.createTransportReject(getInitiator(), getPeer(), getSessionId(),
                        content.getCreator(), content.getName(), newTransport));
                continue;
            }

            //Otherwise, when all went well so far, accept the transport-replace
            content.setTransport(JingleManager.getJingleTransportAdapter(newTransport.getNamespace())
                    .transportFromElement(newTransport));
            responses.add(JingleElement.createTransportAccept(getInitiator(), getPeer(), getSessionId(),
                    content.getCreator(), content.getName(), newTransport));
        }

        //TODO: Put in Thread?
        for (JingleElement response : responses) {
            try {
                jingleManager.getConnection().createStanzaCollectorAndSend(response).nextResultOrThrow();
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                LOGGER.log(Level.SEVERE, "Could not send response to transport-replace: " + e, e);
            }
        }

        return IQ.createResultIQ(request);
    }

    private IQ handleSessionTerminate(JingleElement request) {
        JingleReasonElement reason = request.getReason();

        if (reason == null) {
            throw new AssertionError("Reason MUST not be null! (I guess)...");
        }

        switch (reason.asEnum()) {
            case alternative_session:
            case busy:
            case cancel:
            case connectivity_error:
            case decline:
                // :(
            case expired:
            case failed_application:
            case failed_transport:
            case general_error:
                // well... shit.
            case gone:
            case incompatible_parameters:
            case media_error:
            case security_error:
            case success:
                // Weeeeeh
                break;
            case timeout:
            case unsupported_applications:
            case unsupported_transports:
                break;
            default:
                throw new AssertionError("Unknown reason enum: " + reason.asEnum().toString());
        }
        return IQ.createResultIQ(request);
    }

    private IQ handleTransportReject(JingleElement request) {
        HashMap<JingleContentElement, JingleContent> affectedContents = getAffectedContents(request);

        return null;
    }

    private IQ handleSessionInitiate(JingleElement request) {
        return null;
    }

    private IQ handleTransportInfo(JingleElement request) {
        HashMap<JingleContentElement, JingleContent> affectedContents = getAffectedContents(request);

        for (Map.Entry<JingleContentElement, JingleContent> entry : affectedContents.entrySet()) {

            JingleTransport<?> transport = entry.getValue().getTransport();
            JingleContentTransportInfoElement info = entry.getKey().getTransport().getInfo();
            transport.handleTransportInfo(info, request);
        }

        return IQ.createResultIQ(request);
    }

    private IQ handleTransportAccept(JingleElement request) {
        HashMap<JingleContentElement, JingleContent> affectedContents = getAffectedContents(request);
        for (Map.Entry<JingleContentElement, JingleContent> entry : affectedContents.entrySet()) {

            PendingJingleAction pending = pendingJingleActions.get(entry.getValue());
            if (pending == null) {
                continue;
            }

            if (pending.getAction() != JingleAction.transport_replace) {
                //TODO: Are multiple contents even possible here?
                //TODO: How to react to partially illegal requests?
                return JingleElement.createJingleErrorOutOfOrder(request);
            }

            entry.getValue().setTransport(((PendingJingleAction.TransportReplace) pending).getNewTransport());
        }

        return IQ.createResultIQ(request);
    }

    private IQ handleSessionAccept(JingleElement request) {
        return null;
    }

    private IQ handleSecurityInfo(JingleElement request) {
        HashMap<JingleContentElement, JingleContent> affectedContents = getAffectedContents(request);
        List<JingleElement> responses = new ArrayList<>();

        for (Map.Entry<JingleContentElement, JingleContent> entry : affectedContents.entrySet()) {
            responses.add(entry.getValue().getSecurity().handleSecurityInfo(entry.getKey().getSecurity().getSecurityInfo(), request));
        }

        for (JingleElement response : responses) {
            try {
                getJingleManager().getConnection().createStanzaCollectorAndSend(response).nextResultOrThrow();
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Could not send response to security-info: " + e, e);
            }
        }

        return IQ.createResultIQ(request);
    }

    private IQ handleSessionInfo(JingleElement request) {
        return null;
    }

    private IQ handleDescriptionInfo(JingleElement request) {
        return null;
    }

    private IQ handleContentRemove(JingleElement request) {
        return null;
    }

    private IQ handleContentReject(JingleElement request) {
        return null;
    }

    private IQ handleContentModify(JingleElement request) {
        return null;
    }

    private IQ handleContentAdd(JingleElement request) {
        final List<JingleContentElement> proposedContents = request.getContents();
        final List<JingleContentElement> acceptedContents = new ArrayList<>();

        final HashMap<String, List<JingleContent>> contentsByDescription = new HashMap<>();

        for (JingleContentElement p : proposedContents) {
            JingleContentDescriptionElement description = p.getDescription();
            List<JingleContent> list = contentsByDescription.get(description.getNamespace());
            if (list == null) {
                list = new ArrayList<>();
                contentsByDescription.put(description.getNamespace(), list);
            }
            list.add(JingleContent.fromElement(p));
        }

        for (Map.Entry<String, List<JingleContent>> descriptionCategory : contentsByDescription.entrySet()) {
            JingleDescriptionManager descriptionManager = JingleManager.getInstanceFor(getJingleManager().getConnection()).getDescriptionManager(descriptionCategory.getKey());

            if (descriptionManager == null) {
                //blabla
                continue;
            }

            for (final JingleContent content : descriptionCategory.getValue()) {
                descriptionManager.notifyContentAdd(content);

                ContentAddCallback callback = new ContentAddCallback() {
                    @Override
                    public void acceptContentAdd() {
                        contents.put(content.getName(), content);
                        acceptedContents.add(content.getElement());
                        // TODO: Send content-accept
                    }

                    @Override
                    public void rejectContentAdd() {
                        // TODO: Send content-reject
                    }
                };
                descriptionManager
            }
        }

        if (acceptedContents.size() > 0) {
            JingleElement accept = JingleElement.createContentAccept(getPeer(), getSessionId(), acceptedContents);
            try {
                getJingleManager().getConnection().createStanzaCollectorAndSend(accept).nextResultOrThrow();
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                LOGGER.log(Level.SEVERE, "Could not send response to content-add: " + e, e);
            }
        }

        //TODO: send content-reject for rejected contents!

        return IQ.createResultIQ(request);
    }

    private IQ handleContentAccept(JingleElement request) {
        return null;
    }

    public FullJid getInitiator() {
        return initiator;
    }

    public FullJid getResponder() {
        return responder;
    }

    public FullJid getPeer() {
        return role == Role.initiator ? responder : initiator;
    }

    public FullJid getOurJid() {
        return role == Role.initiator ? initiator : responder;
    }

    public boolean isInitiator() {
        return role == Role.initiator;
    }

    public boolean isResponder() {
        return role == Role.responder;
    }

    public String getSessionId() {
        return sessionId;
    }

    public JingleManager getJingleManager() {
        return jingleManager;
    }

    private HashMap<JingleContentElement, JingleContent> getAffectedContents(JingleElement request) {
        HashMap<JingleContentElement, JingleContent> map = new HashMap<>();
        for (JingleContentElement e : request.getContents()) {
            JingleContent c = contents.get(e.getName());
            if (c == null) {
                throw new AssertionError("Unknown content: " + e.getName());
            }
            map.put(e, c);
        }
        return map;
    }
}