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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.exception.UnsupportedTransportException;
import org.jivesoftware.smackx.jingle.provider.JingleContentSecurityProvider;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.exception.UnsupportedDescriptionException;
import org.jivesoftware.smackx.jingle.exception.UnsupportedSecurityException;
import org.jivesoftware.smackx.jingle.internal.JingleSession;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;

import org.jxmpp.jid.FullJid;

/**
 * Manager for Jingle (XEP-0166).
 */
public class JingleManager extends Manager {
    private static final WeakHashMap<XMPPConnection, JingleManager> INSTANCES = new WeakHashMap<>();

    private static final WeakHashMap<String, JingleContentDescriptionProvider<?>> descriptionProviders = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleContentTransportProvider<?>> transportProviders = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleContentSecurityProvider<?>> securityProviders = new WeakHashMap<>();

    private static final WeakHashMap<String, JingleDescriptionAdapter<?>> descriptionAdapters = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleTransportAdapter<?>> transportAdapters = new WeakHashMap<>();
    private static final WeakHashMap<String, JingleSecurityAdapter<?>> securityAdapters = new WeakHashMap<>();

    private final WeakHashMap<String, JingleDescriptionManager> descriptionManagers = new WeakHashMap<>();
    private final WeakHashMap<String, JingleTransportManager> transportManagers = new WeakHashMap<>();
    private final WeakHashMap<String, JingleSecurityManager> securityManagers = new WeakHashMap<>();

    private final ConcurrentHashMap<FullJidAndSessionId, JingleSession> jingleSessions = new ConcurrentHashMap<>();

    private JingleManager(XMPPConnection connection) {
        super(connection);

        connection.registerIQRequestHandler(
                new AbstractIqRequestHandler(JingleElement.ELEMENT, JingleElement.NAMESPACE, IQ.Type.set, IQRequestHandler.Mode.async) {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest) {
                        final JingleElement jingle = (JingleElement) iqRequest;

                        FullJid fullFrom = jingle.getFrom().asFullJidOrThrow();
                        String sid = jingle.getSid();
                        FullJidAndSessionId fullJidAndSessionId = new FullJidAndSessionId(fullFrom, sid);

                        JingleSession session = jingleSessions.get(fullJidAndSessionId);

                        // We have not seen this session before.
                        // Either it is fresh, or unknown.
                        if (session == null) {

                            if (jingle.getAction() == JingleAction.session_initiate) {
                                //fresh. phew!
                                try {
                                    session = JingleSession.fromSessionInitiate(JingleManager.this, jingle);
                                    jingleSessions.put(fullJidAndSessionId, session);
                                } catch (UnsupportedDescriptionException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_applications);
                                } catch (UnsupportedTransportException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_transports);
                                } catch (UnsupportedSecurityException e) {
                                    e.printStackTrace();
                                    return null;
                                }

                            } else {
                                // Unknown session. Error!
                                return JingleElement.createJingleErrorUnknownSession(jingle);
                            }
                        }

                        return session.handleJingleRequest(jingle);
                    }
                });
    }

    public static JingleManager getInstanceFor(XMPPConnection connection) {
        JingleManager manager = INSTANCES.get(connection);

        if (manager == null) {
            manager = new JingleManager(connection);
            INSTANCES.put(connection, manager);
        }

        return manager;
    }

    public static void registerDescriptionProvider(JingleContentDescriptionProvider<?> provider) {
        descriptionProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentDescriptionProvider<?> getDescriptionProvider(String namespace) {
        return descriptionProviders.get(namespace);
    }

    public static void registerTransportProvider(JingleContentTransportProvider<?> provider) {
        transportProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentTransportProvider<?> getTransportProvider(String namespace) {
        return transportProviders.get(namespace);
    }

    public static void registerSecurityProvider(JingleContentSecurityProvider<?> provider) {
        securityProviders.put(provider.getNamespace(), provider);
    }

    public static JingleContentSecurityProvider<?> getSecurityProvider(String namespace) {
        return securityProviders.get(namespace);
    }

    public static void addJingleDescriptionAdapter(JingleDescriptionAdapter<?> adapter) {
        descriptionAdapters.put(adapter.getNamespace(), adapter);
    }

    public static void addJingleTransportAdapter(JingleTransportAdapter<?> adapter) {
        transportAdapters.put(adapter.getNamespace(), adapter);
    }

    public static void addJingleSecurityAdapter(JingleSecurityAdapter<?> adapter) {
        securityAdapters.put(adapter.getNamespace(), adapter);
    }

    public static JingleDescriptionAdapter<?> getJingleDescriptionAdapter(String namespace) {
        return descriptionAdapters.get(namespace);
    }

    public static JingleTransportAdapter<?> getJingleTransportAdapter(String namespace) {
        return transportAdapters.get(namespace);
    }

    public static JingleSecurityAdapter<?> getJingleSecurityAdapter(String namespace) {
        return securityAdapters.get(namespace);
    }

    public void addJingleDescriptionManager(JingleDescriptionManager manager) {
        descriptionManagers.put(manager.getNamespace(), manager);
    }

    public JingleDescriptionManager getDescriptionManager(String namespace) {
        return descriptionManagers.get(namespace);
    }

    public void addJingleTransportManager(JingleTransportManager manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    public JingleTransportManager getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    public void addJingleSecurityManager(JingleSecurityManager manager) {
        securityManagers.put(manager.getNamespace(), manager);
    }

    public JingleSecurityManager getSecurityManager(String namespace) {
        return securityManagers.get(namespace);
    }

    public List<JingleTransportManager> getAvailableTransportManagers() {
        return getAvailableTransportManagers(Collections.<String>emptySet());
    }

    private List<JingleTransportManager> getAvailableTransportManagers(Set<String> except) {
        Set<String> available = new HashSet<>(transportManagers.keySet());
        available.removeAll(except);
        List<JingleTransportManager> remaining = new ArrayList<>();

        for (String namespace : available) {
            remaining.add(transportManagers.get(namespace));
        }

        return remaining;
    }

    public XMPPConnection getConnection() {
        return connection();
    }
}
