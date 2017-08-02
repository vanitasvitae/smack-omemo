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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.components.JingleContent;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.components.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;

/**
 * Jingle SOCKS5Bytestream transport candidate.
 */
public class JingleS5BTransportCandidate extends JingleTransportCandidate<JingleS5BTransportCandidateElement> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportCandidate.class.getName());

    private final String candidateId;
    private final Bytestream.StreamHost streamHost;
    private final JingleS5BTransportCandidateElement.Type type;

    private Socket socket;

    public JingleS5BTransportCandidate(String candidateId,
                                       Bytestream.StreamHost streamHost,
                                       int priority,
                                       JingleS5BTransportCandidateElement.Type type) {
        this.candidateId = candidateId;
        this.streamHost = streamHost;
        this.type = type;

        setPriority(priority);
    }

    public JingleS5BTransportCandidate(JingleS5BTransportCandidate other) {
        this(other.candidateId,
                other.getStreamHost(),
                other.getPriority(),
                other.type);
    }

    public static JingleS5BTransportCandidate fromElement(JingleS5BTransportCandidateElement element) {
        return new JingleS5BTransportCandidate(element.getCandidateId(), element.getStreamHost(), element.getPriority(), element.getType());
    }

    public String getCandidateId() {
        return candidateId;
    }

    public Bytestream.StreamHost getStreamHost() {
        return streamHost;
    }

    public JingleS5BTransportCandidateElement.Type getType() {
        return type;
    }

    @Override
    public JingleS5BTransportCandidateElement getElement() {
        return new JingleS5BTransportCandidateElement(
                getCandidateId(), getStreamHost().getAddress(),
                getStreamHost().getJID(), getStreamHost().getPort(),
                getPriority(), getType());
    }

    public JingleS5BTransportCandidate connect(int timeout, boolean peersProposal) throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        JingleS5BTransport transport = (JingleS5BTransport) getParent();

        Socks5Client client;
        if (peersProposal) {
            LOGGER.log(Level.INFO, "Connect to foreign candidate " + getCandidateId() + " using " + transport.getDstAddr());
            client = new Socks5Client(getStreamHost(), transport.getDstAddr());
        }
        else {
            LOGGER.log(Level.INFO, "Connect to our candidate " + getCandidateId() + " using " + ((JingleS5BTransport) getParent()).getDstAddr());
            JingleContent content = transport.getParent();
            JingleSession session = content.getParent();
            client = new Socks5ClientForInitiator(getStreamHost(), transport.getDstAddr(), session.getJingleManager().getConnection(), transport.getSid(), session.getPeer());
        }

        this.socket = client.getSocket(timeout);
        return this;
    }

    public Socket getSocket() {
        return socket;
    }
}
