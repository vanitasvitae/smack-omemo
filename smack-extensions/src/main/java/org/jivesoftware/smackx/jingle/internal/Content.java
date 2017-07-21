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
package org.jivesoftware.smackx.jingle.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.Callback;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;

/**
 * Internal class that holds the state of a content in a modifiable form.
 */
public class Content {

    private Session parent;
    private JingleContentElement.Creator creator;
    private String name;
    private String disposition;
    private JingleContentElement.Senders senders;
    private Description description;
    private Transport transport;
    private Security security;

    private final List<Callback> callbacks = Collections.synchronizedList(new ArrayList<Callback>());
    private final Set<String> transportBlacklist = Collections.synchronizedSet(new HashSet<String>());

    public Content(Description description, Transport transport, Security security, String name, String disposition, JingleContentElement.Creator creator, JingleContentElement.Senders senders) {
        this.description = description;
        this.transport = transport;
        this.security = security;
        this.name = name;
        this.disposition = disposition;
        this.creator = creator;
        this.senders = senders;
    }

    public static Content fromElement(JingleContentElement content) {
        Description<?> description = null;
        Transport<?> transport = null;
        Security<?> security = null;

        JingleContentDescriptionElement descriptionElement = content.getDescription();
        if (descriptionElement != null) {
            JingleDescriptionAdapter<?> descriptionAdapter = JingleManager.getJingleDescriptionAdapter(content.getDescription().getNamespace());
            if (descriptionAdapter != null) {
                description = descriptionAdapter.descriptionFromElement(descriptionElement);
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
                throw new AssertionError("DescriptionProvider for " + transportElement.getNamespace() +
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

        return new Content(description, transport, security, content.getName(), content.getDisposition(), content.getCreator(), content.getSenders());
    }

    public void addCallback(Callback callback) {
        callbacks.add(callback);
    }

    public JingleContentElement getElement() {
        return JingleContentElement.getBuilder()
                .setName(name)
                .setCreator(creator)
                .setSenders(senders)
                .setDescription(description.getElement())
                .setTransport(transport.getElement())
                .setSecurity(security.getElement())
                .setDisposition(disposition)
                .build();
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

    public void setParent(Session session) {
        if (this.parent != session) {
            this.parent = session;
        }
    }

    public Session getParent() {
        return parent;
    }

    public Description<?> getDescription() {
        return description;
    }

    public void setDescription(Description<?> description) {
        if (this.description != description) {
            this.description = description;
            description.setParent(this);
        }
    }

    public Transport<?> getTransport() {
        return transport;
    }

    public void setTransport(Transport<?> transport) {
        if (this.transport != transport) {
            this.transport = transport;
            transport.setParent(this);
        }
    }

    public Security<?> getSecurity() {
        return security;
    }

    public void setSecurity(Security<?> security) {
        if (this.security != security) {
            this.security = security;
            security.setParent(this);
        }
    }

    public static String randomName() {
        return "cont-" + StringUtils.randomString(16);
    }
}
