package org.jivesoftware.smackx.jingle3.internal;

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
import org.jivesoftware.smackx.jingle3.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle3.JingleExtensionManager;
import org.jivesoftware.smackx.jingle3.JingleManager;
import org.jivesoftware.smackx.jingle3.Role;
import org.jivesoftware.smackx.jingle3.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle3.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle3.element.JingleAction;
import org.jivesoftware.smackx.jingle3.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedDescriptionException;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedSecurityException;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedTransportException;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 17.07.17.
 */
public class Session {
    private static final Logger LOGGER = Logger.getLogger(Session.class.getName());

    private final ConcurrentHashMap<String, Content> contents = new ConcurrentHashMap<>();
    private final JingleManager jingleManager;

    private final FullJid initiator, responder;
    private final Role role;
    private final String sessionId;

    private final Map<Content, PendingJingleAction> pendingJingleActions =
            Collections.synchronizedMap(new HashMap<Content, PendingJingleAction>());

    public Session(JingleManager manager, FullJid initiator, FullJid responder, Role role, String sessionId) {
        this.jingleManager = manager;
        this.initiator = initiator;
        this.responder = responder;
        this.role = role;
        this.sessionId = sessionId;
    }

    void addContent(Content content) {
        contents.put(content.getName(), content);
        content.setSession(this);
    }

    void addContent(JingleContentElement content)
            throws UnsupportedSecurityException, UnsupportedTransportException, UnsupportedDescriptionException {
        addContent(Content.fromElement(content));
    }

    public static Session fromSessionInitiate(JingleManager manager, JingleElement initiate)
            throws UnsupportedSecurityException, UnsupportedDescriptionException, UnsupportedTransportException {
        if (initiate.getAction() != JingleAction.session_initiate) {
            throw new IllegalArgumentException("Jingle-Action MUST be 'session-initiate'.");
        }

        Session session = new Session(manager, initiate.getInitiator(), initiate.getResponder(), Role.responder, initiate.getSid());
        List<JingleContentElement> initiateContents = initiate.getContents();

        for (JingleContentElement content : initiateContents) {
            session.addContent(content);
        }

        return session;
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
            Content content = contents.get(affected.getName());
            JingleContentTransportElement newTransport = affected.getTransport();
            Set<String> blacklist = content.getTransportBlacklist();

            // Proposed transport method might already be on the blacklist (eg. because of previous failures)
            if (blacklist.contains(newTransport.getNamespace())) {
                responses.add(JingleElement.createTransportReject(getInitiator(), getPeer(), getSessionId(),
                        content.getCreator(), content.getName(), newTransport));
                continue;

            }

            JingleTransportAdapter<?> transportAdapter = JingleExtensionManager.getJingleTransportAdapter(
                    newTransport.getNamespace());
            // This might be an unknown transport.
            if (transportAdapter == null) {
                responses.add(JingleElement.createTransportReject(getInitiator(), getPeer(), getSessionId(),
                        content.getCreator(), content.getName(), newTransport));
                continue;
            }

            //Otherwise, when all went well so far, accept the transport-replace
            content.setTransport(JingleExtensionManager.getJingleTransportAdapter(newTransport.getNamespace())
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
                throw new AssertionError("Unknown reson enum: " + reason.asEnum().toString());
        }
        return IQ.createResultIQ(request);
    }

    private IQ handleTransportReject(JingleElement request) {
        HashMap<JingleContentElement, Content> affectedContents = getAffectedContents(request);

        return null;
    }

    private IQ handleSessionInitiate(JingleElement request) {
        return null;
    }

    private IQ handleTransportInfo(JingleElement request) {
        HashMap<JingleContentElement, Content> affectedContents = getAffectedContents(request);
        List<JingleElement> responses = new ArrayList<>();

        for (Map.Entry<JingleContentElement, Content> entry : affectedContents.entrySet()) {
            responses.add(entry.getValue().getTransport().handleTransportInfo(entry.getKey().getTransport().getInfo()));
        }

        for (JingleElement response : responses) {
            try {
                getJingleManager().getConnection().createStanzaCollectorAndSend(response).nextResultOrThrow();
            } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
                LOGGER.log(Level.SEVERE, "Could not send response to transport-info: " + e, e);
            }
        }

        return IQ.createResultIQ(request);
    }

    private IQ handleTransportAccept(JingleElement request) {
        HashMap<JingleContentElement, Content> affectedContents = getAffectedContents(request);
        for (Map.Entry<JingleContentElement, Content> entry : affectedContents.entrySet()) {

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
        HashMap<JingleContentElement, Content> affectedContents = getAffectedContents(request);
        List<JingleElement> responses = new ArrayList<>();

        for (Map.Entry<JingleContentElement, Content> entry : affectedContents.entrySet()) {
            responses.add(entry.getValue().getSecurity().handleSecurityInfo(entry.getKey().getSecurity().getSecurityInfo()));
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

        final HashMap<String, List<Content>> contentsByDescription = new HashMap<>();

        for (JingleContentElement p : proposedContents) {
            JingleContentDescriptionElement description = p.getDescription();
            List<Content> list = contentsByDescription.get(description.getNamespace());
            if (list == null) {
                list = new ArrayList<>();
                contentsByDescription.put(description.getNamespace(), list);
            }
            list.add(Content.fromElement(p));
        }

        for (Map.Entry<String, List<Content>> descriptionCategory : contentsByDescription.entrySet()) {
            JingleDescriptionManager descriptionManager = JingleExtensionManager.getInstanceFor(getJingleManager().getConnection()).getDescriptionManager(descriptionCategory.getKey());

            if (descriptionManager == null) {
                //blabla
                continue;
            }

            for (final Content content : descriptionCategory.getValue()) {
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
                descriptionManager.notifyContentListeners(content, callback);
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

    public String getSessionId() {
        return sessionId;
    }

    public JingleManager getJingleManager() {
        return jingleManager;
    }

    private HashMap<JingleContentElement, Content> getAffectedContents(JingleElement request) {
        HashMap<JingleContentElement, Content> map = new HashMap<>();
        for (JingleContentElement e : request.getContents()) {
            Content c = contents.get(e.getName());
            if (c == null) {
                throw new AssertionError("Unknown content: " + e.getName());
            }
            map.put(e, c);
        }
        return map;
    }
}