package org.jivesoftware.smackx.jingle_ibb2;

import java.io.IOException;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle.JingleBytestreamManager;
import org.jivesoftware.smackx.jingle.JingleTransportManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_ibb2.element.JingleIBBTransport;
import org.jivesoftware.smackx.jingle_ibb2.provider.JingleIBBTransportProvider;
import org.jxmpp.jid.FullJid;

/**
 * BytestreamManager for Jingle InBandBytestream Transports.
 */
public final class JingleIBBTransportManager extends JingleBytestreamManager<JingleIBBTransport> {

    private static final WeakHashMap<XMPPConnection, JingleIBBTransportManager> INSTANCES = new WeakHashMap<>();

    public static JingleIBBTransportManager getInstanceFor(XMPPConnection connection) {
        JingleIBBTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleIBBTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private JingleIBBTransportManager(XMPPConnection connection) {
        super(connection);
    }

    @Override
    public Jingle createSessionInitiate(FullJid targetJID, JingleContentDescription application) throws XMPPException, IOException, InterruptedException, SmackException {
        return createSessionInitiate(targetJID, application, JingleTransportManager.generateRandomId());
    }

    @Override
    public Jingle createSessionInitiate(FullJid targetJID, JingleContentDescription application, String sessionID) throws XMPPException, IOException, InterruptedException, SmackException {
        Jingle.Builder jb = Jingle.getBuilder();
        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setDescription(application)
                .setName(JingleTransportManager.generateRandomId())
                .setCreator(JingleContent.Creator.initiator)
                .setSenders(JingleContent.Senders.initiator)
                .addTransport(new JingleIBBTransport());
        jb.addJingleContent(cb.build());
        jb.setInitiator(connection().getUser())
                .setResponder(targetJID)
                .setSessionId(sessionID)
                .setAction(JingleAction.session_initiate);
        Jingle jingle = jb.build();
        jingle.setTo(targetJID);
        jingle.setFrom(connection().getUser());
        jingle.setType(IQ.Type.set);
        return jingle;
    }

    @Override
    public Jingle createSessionAccept(Jingle request) {
        Jingle.Builder jb = Jingle.getBuilder();
        jb.setAction(JingleAction.session_accept)
                .setSessionId(request.getSid())
                .setResponder(connection().getUser());

        JingleContent requestContent = request.getContents().get(0);
        JingleContent.Builder cb = JingleContent.getBuilder();
        cb.setDescription(requestContent.getDescription())
                .setSenders(requestContent.getSenders())
                .setCreator(requestContent.getCreator())
                .setName(requestContent.getName());
        cb.addTransport(requestContent.getJingleTransports().get(0));

        jb.addJingleContent(cb.build());
        Jingle jingle = jb.build();
        jingle.setType(IQ.Type.set);
        jingle.setTo(request.getFrom());
        jingle.setFrom(connection().getUser());
        return jingle;
    }

    @Override
    public BytestreamSession outgoingInitiatedSession(Jingle jingle) throws Exception {
        InBandBytestreamManager ibb = InBandBytestreamManager.getByteStreamManager(connection());
        return ibb.establishSession(jingle.getResponder(), jingle.getSid());
    }

    @Override
    public void setIncomingRespondedSessionListener(Jingle jingle, BytestreamListener listener) {
        InBandBytestreamManager.getByteStreamManager(connection()).addIncomingBytestreamListener(listener);
    }

    //###############################################


    @Override
    protected JingleContentTransportProvider<JingleIBBTransport> createJingleContentTransportProvider() {
        return new JingleIBBTransportProvider();
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE_V1;
    }
}
