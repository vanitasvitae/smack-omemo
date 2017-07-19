package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Transport<D extends JingleContentTransportElement> {

    public abstract D getElement();

    public abstract String getNamespace();

    public abstract void establishIncomingBytestreamSession(FullJid peer,
                                                            String transportSessionId,
                                                            BytestreamSessionEstablishedListener listener,
                                                            XMPPConnection connection);

    public abstract void establishOutgoingBytestreamSession(FullJid peer,
                                                            String transportSessionId,
                                                            BytestreamSessionEstablishedListener listener,
                                                            XMPPConnection connection);

    public abstract void setPeersProposal(Transport<?> peersProposal);

    public abstract void handleTransportInfo(JingleContentTransportInfoElement info);
}
