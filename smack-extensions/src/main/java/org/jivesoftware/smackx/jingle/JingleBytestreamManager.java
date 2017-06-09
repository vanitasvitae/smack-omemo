package org.jivesoftware.smackx.jingle;

import java.io.IOException;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 09.06.17.
 */
public abstract class JingleBytestreamManager<D extends JingleContentTransport>
        extends Manager {

    public JingleBytestreamManager(XMPPConnection connection) {
        super(connection);
        JingleTransportManager.getInstanceFor(connection).registerJingleContentTransportManager(this);
        JingleContentProviderManager.addJingleContentTransportProvider(getNamespace(), createJingleContentTransportProvider());
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
    }

    protected abstract JingleContentTransportProvider<D> createJingleContentTransportProvider();

    public abstract String getNamespace();

    public Jingle createSessionInitiate(FullJid targetJID, JingleContentDescription application) throws XMPPException, IOException, InterruptedException, SmackException {
        return createSessionInitiate(targetJID, application, JingleTransportManager.generateRandomId());
    }

    public abstract Jingle createSessionInitiate(FullJid targetJID, JingleContentDescription application, String sessionId) throws XMPPException, IOException, InterruptedException, SmackException;

    public abstract Jingle createSessionAccept(Jingle request);

    public abstract BytestreamSession outgoingInitiatedSession(Jingle jingle) throws Exception;

    public abstract void setIncomingRespondedSessionListener(Jingle jingle, BytestreamListener listener);

}
