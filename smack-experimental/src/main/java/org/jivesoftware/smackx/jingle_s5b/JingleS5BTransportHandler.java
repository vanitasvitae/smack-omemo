/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_s5b;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * JingleTransportHandler for Socks5Bytestreams.
 */
public class JingleS5BTransportHandler implements JingleTransportHandler<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportHandler.class.getName());

    private final WeakReference<JingleSessionHandler> sessionHandler;

    private JingleS5BTransportCandidate receivedCandidateUsed = null;

    private Socket connectedSocket = null;

    public JingleS5BTransportHandler(JingleSessionHandler sessionHandler) {
        this.sessionHandler = new WeakReference<>(sessionHandler);
    }

    @Override
    public void establishOutgoingSession(JingleManager.FullJidAndSessionId fullJidAndSessionId,
                                         JingleContent content,
                                         JingleTransportEstablishedCallback callback) {
        JingleContentTransport hopefullyS5BTransport = content.getJingleTransports().get(0);
        if (!hopefullyS5BTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Transport must be a JingleS5BTransport.");
        }

        JingleS5BTransport transport = (JingleS5BTransport) hopefullyS5BTransport;

        Socks5Proxy.getSocks5Proxy().addLocalAddress(Socks5Utils.createDigest(
                fullJidAndSessionId.getSessionId(),                 //SessionID
                getConnection().getUser().asFullJidIfPossible(),    //Us
                fullJidAndSessionId.getFullJid()));                 //Them

        JingleS5BTransportCandidate usedCandidate = null;
        for (JingleContentTransportCandidate c : transport.getCandidates()) {
            usedCandidate = (JingleS5BTransportCandidate) c;
            Bytestream.StreamHost streamHost = usedCandidate.getStreamHost();
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            LOGGER.log(Level.INFO, "Connect outwards to " + address);
            // establish socket
            try {

                // build SOCKS5 client
                final Socks5Client socks5Client = new Socks5Client(streamHost, transport.getDestinationAddress());

                // connect to SOCKS5 proxy with a timeout
                connectedSocket = socks5Client.getSocket(10 * 1000);


                // set selected host
                break;

            }
            catch (TimeoutException | IOException | SmackException | XMPPException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not connect outwards to " + address + ": " + e, e);
            }
        }

        if (connectedSocket != null) {
            Jingle.Builder jb = Jingle.getBuilder();
            jb.setSessionId(fullJidAndSessionId.getSessionId())
                    .setAction(JingleAction.transport_info)
                    .setInitiator(getConnection().getUser());

            JingleContent.Builder cb = JingleContent.getBuilder();
            cb.setName(content.getName())
                    .setCreator(content.getCreator())
                    .setSenders(content.getSenders());

            JingleS5BTransport.Builder tb = JingleS5BTransport.getBuilder();
            tb.addTransportInfo(JingleS5BTransportInfo.CandidateUsed(usedCandidate.getCandidateId()));
            cb.addTransport(tb.build());

            jb.addJingleContent(cb.build());

            Jingle jingle = jb.build();
            jingle.setFrom(getConnection().getUser());
            jingle.setTo(fullJidAndSessionId.getFullJid());
            try {
                getConnection().sendStanza(jingle);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-used stanza: " + e, e);
            }
        }
    }

    @Override
    public void establishIncomingSession(JingleManager.FullJidAndSessionId fullJidAndSessionId,
                                         JingleContent content,
                                         JingleTransportEstablishedCallback callback) {
        JingleContentTransport hopefullyS5BTransport = content.getJingleTransports().get(0);
        if (!hopefullyS5BTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Transport must be a JingleS5BTransport.");
        }
        JingleS5BTransport transport = (JingleS5BTransport) hopefullyS5BTransport;



        ArrayList<Bytestream.StreamHost> streamHosts = new ArrayList<>();
        for (JingleContentTransportCandidate c : transport.getCandidates()) { //TODO Sort
            streamHosts.add(((JingleS5BTransportCandidate) c).getStreamHost());
        }

        for (Bytestream.StreamHost streamHost : streamHosts) {
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            LOGGER.log(Level.INFO, "Connect inwards to " + address);
            // establish socket
            try {

                // build SOCKS5 client
                final Socks5Client socks5Client = new Socks5Client(streamHost, transport.getDestinationAddress());

                // connect to SOCKS5 proxy with a timeout
                Socket socket = socks5Client.getSocket(10 * 1000);

                // set selected host
                break;

            }
            catch (TimeoutException | IOException | SmackException | XMPPException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not connect inwards to " + address + ": " + e, e);
            }

        }
    }

    @Override
    public XMPPConnection getConnection() {
        JingleSessionHandler handler = sessionHandler.get();
        return handler != null ? handler.getConnection() : null;
    }

    public void setReceivedCandidateUsed(JingleS5BTransportCandidate used) {
        this.receivedCandidateUsed = used;
    }
}
