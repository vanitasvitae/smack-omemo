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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.Callback;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

/**
 * Internal class that holds the state of a content in a modifiable form.
 */
public class JingleContent implements JingleTransportCallback {

    private static final Logger LOGGER = Logger.getLogger(JingleContent.class.getName());

    private JingleSession parent;
    private JingleContentElement.Creator creator;
    private String name;
    private String disposition;
    private JingleContentElement.Senders senders;
    private JingleDescription<?> description;
    private JingleTransport<?> transport;
    private JingleSecurity<?> security;

    private final List<Callback> callbacks = Collections.synchronizedList(new ArrayList<Callback>());
    private final Set<String> transportBlacklist = Collections.synchronizedSet(new HashSet<String>());

    public IQ handleJingleRequest(JingleElement request, XMPPConnection connection) {
        switch (request.getAction()) {
            case content_modify:
                return handleContentModify(request, connection);
            case description_info:
                return handleDescriptionInfo(request, connection);
            case security_info:
                return handleSecurityInfo(request, connection);
            case session_info:
                return handleSessionInfo(request, connection);
            case transport_accept:
                return handleTransportAccept(request, connection);
            case transport_info:
                return handleTransportInfo(request, connection);
            case transport_reject:
                return handleTransportReject(request, connection);
            case transport_replace:
                return handleTransportReplace(request, connection);
            default:
                throw new AssertionError("Illegal jingle action: " + request.getAction() + " is not allowed here.");
        }
    }

    public IQ handleSessionAccept(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleContentModify(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleDescriptionInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public void handleContentRemove(JingleSession session, XMPPConnection connection) {

    }

    public IQ handleSecurityInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleSessionInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleTransportAccept(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleTransportInfo(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleTransportReject(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public IQ handleTransportReplace(JingleElement request, XMPPConnection connection) {
        return IQ.createResultIQ(request);
    }

    public enum STATE {
        pending_accept,
        pending_transmission_start,
        pending_transport_replace,
        transmission_in_progress,
        transmission_successful,
        transmission_failed,
        transmission_cancelled
    }

    public JingleContent(JingleContentElement.Creator creator, JingleContentElement.Senders senders) {
        this(null, null, null, randomName(), null, creator, senders);
    }

    public JingleContent(JingleDescription<?> description, JingleTransport<?> transport, JingleSecurity<?> security, String name, String disposition, JingleContentElement.Creator creator, JingleContentElement.Senders senders) {
        setDescription(description);
        setTransport(transport);
        setSecurity(security);
        this.name = name;
        this.disposition = disposition;
        this.creator = creator;
        this.senders = senders;
    }

    public static JingleContent fromElement(JingleContentElement content) {
        JingleDescription<?> description = null;
        JingleTransport<?> transport = null;
        JingleSecurity<?> security = null;

        JingleContentDescriptionElement descriptionElement = content.getDescription();
        if (descriptionElement != null) {
            JingleDescriptionAdapter<?> descriptionAdapter = JingleManager.getJingleDescriptionAdapter(content.getDescription().getNamespace());
            if (descriptionAdapter != null) {
                description = descriptionAdapter.descriptionFromElement(content.getCreator(), content.getSenders(), content.getName(), content.getDisposition(), descriptionElement);
            } else {
                throw new AssertionError("DescriptionProvider for " + descriptionElement.getNamespace() +
                        " seems to be registered, but no corresponding JingleDescriptionAdapter was found.");
            }
        }

        JingleContentTransportElement transportElement = content.getTransport();
        if (transportElement != null) {
            JingleTransportAdapter<?> transportAdapter = JingleManager.getJingleTransportAdapter(content.getTransport().getNamespace());
            if (transportAdapter != null) {
                transport = transportAdapter.transportFromElement(transportElement);
            } else {
                throw new AssertionError("TransportProvider for " + transportElement.getNamespace() +
                        " seems to be registered, but no corresponding JingleTransportAdapter was found.");
            }
        }

        JingleContentSecurityElement securityElement = content.getSecurity();
        if (securityElement != null) {
            JingleSecurityAdapter<?> securityAdapter = JingleManager.getJingleSecurityAdapter(content.getSecurity().getNamespace());
            if (securityAdapter != null) {
                security = securityAdapter.securityFromElement(securityElement);
            } else {
                throw new AssertionError("SecurityProvider for " + securityElement.getNamespace() +
                        " seems to be registered, but no corresponding JingleSecurityAdapter was found.");
            }
        }

        return new JingleContent(description, transport, security, content.getName(), content.getDisposition(), content.getCreator(), content.getSenders());
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public JingleContentElement getElement() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder()
                .setName(name)
                .setCreator(creator)
                .setSenders(senders)
                .setDescription(description.getElement())
                .setTransport(transport.getElement())
                .setDisposition(disposition);

        if (security != null) {
            builder.setSecurity(security.getElement());
        }

        return builder.build();
    }

    public Set<String> getTransportBlacklist() {
        return transportBlacklist;
    }

    public JingleContentElement.Creator getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    public JingleContentElement.Senders getSenders() {
        return senders;
    }

    public void setParent(JingleSession session) {
        if (this.parent != session) {
            this.parent = session;
        }
    }

    public JingleSession getParent() {
        return parent;
    }

    public JingleDescription<?> getDescription() {
        return description;
    }

    public void setDescription(JingleDescription<?> description) {
        if (description != null && this.description != description) {
            this.description = description;
            description.setParent(this);
        }
    }

    public JingleTransport<?> getTransport() {
        return transport;
    }

    public void setTransport(JingleTransport<?> transport) {
        if (transport != null && this.transport != transport) {
            this.transport = transport;
            transport.setParent(this);
        }
    }

    public JingleSecurity<?> getSecurity() {
        return security;
    }

    public void setSecurity(JingleSecurity<?> security) {
        if (security != null && this.security != security) {
            this.security = security;
            security.setParent(this);
        }
    }

    private boolean isSending() {
        return (getSenders() == JingleContentElement.Senders.initiator && getParent().isInitiator()) ||
                (getSenders() == JingleContentElement.Senders.responder && getParent().isResponder()) ||
                getSenders() == JingleContentElement.Senders.both;
    }

    private boolean isReceiving() {
        return (getSenders() == JingleContentElement.Senders.initiator && getParent().isResponder()) ||
                (getSenders() == JingleContentElement.Senders.responder && getParent().isInitiator()) ||
                getSenders() == JingleContentElement.Senders.both;
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {

        if (bytestreamSession == null) {
            throw new AssertionError("bytestreamSession MUST NOT be null at this point.");
        }

        description.onTransportReady(bytestreamSession);
    }

    @Override
    public void onTransportFailed(Exception e) {
        //Add current transport to blacklist.
        getTransportBlacklist().add(transport.getNamespace());

        //Replace transport.
        if (getParent().isInitiator()) {
            try {
                replaceTransport(getTransportBlacklist(), getParent().getJingleManager().getConnection());
            } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException | XMPPException.XMPPErrorException e1) {
                LOGGER.log(Level.SEVERE, "Could not send transport-replace: " + e, e);
            }
        }
    }

    private void replaceTransport(Set<String> blacklist, XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        JingleSession session = getParent();
        JingleManager jingleManager = session.getJingleManager();

        JingleTransportManager rManager = jingleManager.getBestAvailableTransportManager(blacklist);
        if (rManager == null) {
            JingleElement failedTransport = JingleElement.createSessionTerminate(session.getPeer(),
                    session.getSessionId(), JingleReasonElement.Reason.failed_transport);
            connection.createStanzaCollectorAndSend(failedTransport).nextResultOrThrow();
            return;
        }

        JingleTransport<?> rTransport = rManager.createTransport(this);

        JingleElement transportReplace = JingleElement.createTransportReplace(session.getInitiator(), session.getPeer(),
                session.getSessionId(), getCreator(), getName(), rTransport.getElement());

        connection.createStanzaCollectorAndSend(transportReplace).nextResultOrThrow();
    }

    public void handleContentAccept(JingleElement request, XMPPConnection connection) {
        //Establish transport
        try {
            if (isReceiving()) {
                getTransport().establishIncomingBytestreamSession(connection, this, getParent());
            } else if (isSending()) {
                getTransport().establishOutgoingBytestreamSession(connection, this, getParent());
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error establishing connection: " + e, e);
        }
    }

    public static String randomName() {
        return "cont-" + StringUtils.randomString(16);
    }
}
