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
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Class that represents a contents transport component.
 */
public abstract class JingleTransport<D extends JingleContentTransportElement> extends SmackFuture<BytestreamSession> {

    private JingleContent parent;
    private final ArrayList<JingleTransportCandidate<?>> candidates = new ArrayList<>();

    private JingleTransport<?> peersProposal;
    private boolean isPeersProposal;

    protected BytestreamSession bytestreamSession;

    public abstract D getElement();

    public void addCandidate(JingleTransportCandidate<?> candidate) {
        // Insert sorted by descending priority

        // Empty list -> insert
        if (candidates.isEmpty()) {
            candidates.add(candidate);
            candidate.setParent(this);
            return;
        }

        // Find appropriate index
        for (int i = 0; i < candidates.size(); i++) {
            JingleTransportCandidate<?> c = candidates.get(i);

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

    public List<JingleTransportCandidate<?>> getCandidates() {
        return candidates;
    }

    public abstract String getNamespace();

    public abstract void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    public abstract void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    public void setPeersProposal(JingleTransport<?> peersProposal) {
        this.peersProposal = peersProposal;
        peersProposal.isPeersProposal = true;
    }

    public boolean isPeersProposal() {
        return isPeersProposal;
    }

    public JingleTransport<?> getPeersProposal() {
        return peersProposal;
    }

    public abstract IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping);

    public void setParent(JingleContent parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public JingleContent getParent() {
        return parent;
    }

    public BytestreamSession getBytestreamSession() {
        return bytestreamSession;
    }
}
