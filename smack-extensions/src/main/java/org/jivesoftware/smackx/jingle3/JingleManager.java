package org.jivesoftware.smackx.jingle3;

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.FullJidAndSessionId;
import org.jivesoftware.smackx.jingle3.element.JingleAction;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedDescriptionException;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedSecurityException;
import org.jivesoftware.smackx.jingle3.exception.UnsupportedTransportException;
import org.jivesoftware.smackx.jingle3.internal.Session;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 18.07.17.
 */
public class JingleManager extends Manager {
    private static final WeakHashMap<XMPPConnection, JingleManager> INSTANCES = new WeakHashMap<>();

    private final ConcurrentHashMap<FullJidAndSessionId, Session> jingleSessions = new ConcurrentHashMap<>();

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

                        Session session = jingleSessions.get(fullJidAndSessionId);

                        // We have not seen this session before.
                        // Either it is fresh, or unknown.
                        if (session == null) {

                            if (jingle.getAction() == JingleAction.session_initiate) {
                                //fresh. phew!
                                try {
                                    session = Session.fromSessionInitiate(JingleManager.this, jingle);
                                } catch (UnsupportedDescriptionException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_applications);
                                } catch (UnsupportedTransportException e) {
                                    return JingleElement.createSessionTerminate(jingle.getFrom().asFullJidOrThrow(),
                                            jingle.getSid(), JingleReasonElement.Reason.unsupported_transports);
                                } catch (UnsupportedSecurityException e) {
                                    e.printStackTrace();
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

    public XMPPConnection getConnection() {
        return connection();
    }
}
