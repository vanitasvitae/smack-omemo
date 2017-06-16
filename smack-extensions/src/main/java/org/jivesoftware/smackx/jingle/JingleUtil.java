package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jxmpp.jid.FullJid;

/**
 * Util to quickly send jingle stanzas.
 */
public class JingleUtil {

    private final XMPPConnection connection;

    public JingleUtil(XMPPConnection connection) {
        this.connection = connection;
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

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
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

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    private IQ sendSessionTerminate(FullJid recipient, String sessionId, JingleReason.Reason reason)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        return sendSessionTerminate(recipient, sessionId, new JingleReason(reason));
    }

    private IQ sendSessionTerminate(FullJid recipient, String sessionId, JingleReason reason)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId)
                .setReason(reason);

        Jingle jingle = jb.build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }

    public IQ sendSessionTerminateDecline(FullJid recipient,
                                    String sessionId)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {
        return sendSessionTerminate(recipient, sessionId, JingleReason.Reason.decline);
    }

    public IQ sendSessionTerminateSuccess(FullJid recipient,
                                      String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return sendSessionTerminate(recipient, sessionId, JingleReason.Reason.success);
    }

    public IQ sendSessionTerminateBusy(FullJid recipient,
                                String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return sendSessionTerminate(recipient, sessionId, JingleReason.Reason.busy);
    }

    public IQ sendSessionTerminateAlternativeSession(FullJid recipient,
                                              String sessionId,
                                              String altSessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return sendSessionTerminate(recipient, sessionId, JingleReason.AlternativeSession(altSessionId));
    }

    public IQ sendSessionTerminateCancel(FullJid recipient,
                                  String sessionId)
            throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        return sendSessionTerminate(recipient, sessionId, JingleReason.Reason.cancel);
    }

    public IQ sendSessionTerminateContentCancel(FullJid recipient,
                                  String sessionId,
                                  JingleContent.Creator contentCreator,
                                  String contentName)
            throws SmackException.NotConnectedException, InterruptedException,
            XMPPException.XMPPErrorException, SmackException.NoResponseException {

        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_terminate)
                .setSessionId(sessionId);

        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setCreator(contentCreator).setName(contentName);

        Jingle jingle = jb.addJingleContent(cb.build()).build();
        jingle.setFrom(connection.getUser());
        jingle.setTo(recipient);

        return connection.createStanzaCollectorAndSend(jingle).nextResultOrThrow();
    }
}
