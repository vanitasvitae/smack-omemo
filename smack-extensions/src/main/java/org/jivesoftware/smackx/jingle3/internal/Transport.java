package org.jivesoftware.smackx.jingle3.internal;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle3.transport.BytestreamSessionEstablishedListener;

import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Transport<D extends JingleContentTransportElement> {

    private Content parent;
    private final ArrayList<TransportCandidate<?>> candidates = new ArrayList<>();

    private Transport peersProposal;
    private boolean isPeersProposal;

    public abstract D getElement();

    public void addCandidate(TransportCandidate<?> candidate) {
        // Insert sorted by priority descending

        // Empty list -> insert
        if (candidates.isEmpty()) {
            candidates.add(candidate);
            candidate.setParent(this);
            return;
        }

        // Find appropriate index
        for (int i = 0; i < candidates.size(); i++) {
            TransportCandidate<?> c = candidates.get(i);

            // list already contains element -> return
            if (c == candidate) {
                return;
            }

            //Found the index
            if (c.getPriority() <= candidate.getPriority()) {
                candidates.add(i, candidate);
                candidate.setParent(this);
            }
        }
    }

    public List<TransportCandidate<?>> getCandidates() {
        return candidates;
    }

    public abstract String getNamespace();

    public abstract void establishIncomingBytestreamSession(FullJid peer,
                                                            String transportSessionId,
                                                            BytestreamSessionEstablishedListener listener,
                                                            XMPPConnection connection);

    public abstract void establishOutgoingBytestreamSession(FullJid peer,
                                                            String transportSessionId,
                                                            BytestreamSessionEstablishedListener listener,
                                                            XMPPConnection connection);

    public void setPeersProposal(Transport peersProposal) {
        this.peersProposal = peersProposal;
        peersProposal.isPeersProposal = true;
    }

    public boolean isPeersProposal() {
        return isPeersProposal;
    }

    public abstract void handleTransportInfo(JingleContentTransportInfoElement info);

    public void setParent(Content parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public Content getParent() {
        return parent;
    }
}
