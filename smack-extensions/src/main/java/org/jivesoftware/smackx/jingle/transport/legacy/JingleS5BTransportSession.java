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
package org.jivesoftware.smackx.jingle.transport.legacy;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;

/**
 * Handler that handles Jingle Socks5Bytestream transports (XEP-0260).
 */
public class JingleS5BTransportSession extends JingleTransportSession<JingleS5BTransportElement> {


    private void initiateSession() {
        Socks5Proxy.getSocks5Proxy().addTransfer(createTransport().getDestinationAddress());
        JingleContentElement content = jingleSession.getContents().get(0);
        UsedCandidate usedCandidate = chooseFromProposedCandidates(theirProposal);
        if (usedCandidate == null) {
            ourChoice = CANDIDATE_FAILURE;
            JingleElement candidateError = transportManager().createCandidateError(
                    jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                    content.getSenders(), content.getCreator(), content.getName(), theirProposal.getStreamId());
            try {
                jingleSession.getConnection().sendStanza(candidateError);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-error.", e);
            }
        } else {
            ourChoice = usedCandidate;
            JingleElement jingle = transportManager().createCandidateUsed(jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                    content.getSenders(), content.getCreator(), content.getName(), theirProposal.getStreamId(), ourChoice.candidate.getCandidateId());
            try {
                jingleSession.getConnection().createStanzaCollectorAndSend(jingle)
                        .nextResultOrThrow();
            } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-used.", e);
            }
        }
        connectIfReady();
    }

    private UsedCandidate chooseFromProposedCandidates(JingleS5BTransportElement proposal) {
        for (JingleContentTransportCandidateElement c : proposal.getCandidates()) {
            JingleS5BTransportCandidateElement candidate = (JingleS5BTransportCandidateElement) c;

            try {
                return connectToTheirCandidate(candidate);
            } catch (InterruptedException | TimeoutException | XMPPException | SmackException | IOException e) {
                LOGGER.log(Level.WARNING, "Could not connect to " + candidate.getHost(), e);
            }
        }
        LOGGER.log(Level.WARNING, "Failed to connect to any candidate.");
        return null;
    }

    public IQ handleCandidateUsed(JingleElement jingle) {
        JingleS5BTransportInfoElement info = (JingleS5BTransportInfoElement) jingle.getContents().get(0).getTransport().getInfo();
        String candidateId = ((JingleS5BTransportInfoElement.CandidateUsed) info).getCandidateId();
        theirChoice = new UsedCandidate(ourProposal, ourProposal.getCandidate(candidateId), null);

        if (theirChoice.candidate == null) {
            /*
            TODO: Booooooh illegal candidateId!! Go home!!!!11elf
             */
        }

        connectIfReady();

        return IQ.createResultIQ(jingle);
    }

    public IQ handleCandidateActivate(JingleElement jingle) {
        LOGGER.log(Level.INFO, "handleCandidateActivate");
        Socks5BytestreamSession bs = new Socks5BytestreamSession(ourChoice.socket,
                ourChoice.candidate.getJid().asBareJid().equals(jingleSession.getRemote().asBareJid()));
        callback.onSessionInitiated(bs);
        return IQ.createResultIQ(jingle);
    }

    public IQ handleCandidateError(JingleElement jingle) {
        theirChoice = CANDIDATE_FAILURE;
        connectIfReady();
        return IQ.createResultIQ(jingle);
    }

    public IQ handleProxyError(JingleElement jingle) {
        //TODO
        return IQ.createResultIQ(jingle);
    }

    /**
     * Determine, which candidate (ours/theirs) is the nominated one.
     * Connect to this candidate. If it is a proxy and it is ours, activate it and connect.
     * If its a proxy and it is theirs, wait for activation.
     * If it is not a proxy, just connect.
     */
    private void connectIfReady() {
        JingleContentElement content = jingleSession.getContents().get(0);
        if (ourChoice == null || theirChoice == null) {
            // Not yet ready.
            LOGGER.log(Level.INFO, "Not ready.");
            return;
        }

        if (ourChoice == CANDIDATE_FAILURE && theirChoice == CANDIDATE_FAILURE) {
            LOGGER.log(Level.INFO, "Failure.");
            jingleSession.onTransportMethodFailed(getNamespace());
            return;
        }

        LOGGER.log(Level.INFO, "Ready.");

        //Determine nominated candidate.
        UsedCandidate nominated;
        if (ourChoice != CANDIDATE_FAILURE && theirChoice != CANDIDATE_FAILURE) {
            if (ourChoice.candidate.getPriority() > theirChoice.candidate.getPriority()) {
                nominated = ourChoice;
            } else if (ourChoice.candidate.getPriority() < theirChoice.candidate.getPriority()) {
                nominated = theirChoice;
            } else {
                nominated = jingleSession.isInitiator() ? ourChoice : theirChoice;
            }
        } else if (ourChoice != CANDIDATE_FAILURE) {
            nominated = ourChoice;
        } else {
            nominated = theirChoice;
        }

        if (nominated == theirChoice) {
            LOGGER.log(Level.INFO, "Their choice, so our proposed candidate is used.");
            boolean isProxy = nominated.candidate.getType() == JingleS5BTransportCandidateElement.Type.proxy;
            try {
                nominated = connectToOurCandidate(nominated.candidate);
            } catch (InterruptedException | IOException | XMPPException | SmackException | TimeoutException e) {
                LOGGER.log(Level.INFO, "Could not connect to our candidate.", e);
                //TODO: Proxy-Error
                return;
            }

            if (isProxy) {
                LOGGER.log(Level.INFO, "Is external proxy. Activate it.");
                Bytestream activate = new Bytestream(ourProposal.getStreamId());
                activate.setMode(null);
                activate.setType(IQ.Type.set);
                activate.setTo(nominated.candidate.getJid());
                activate.setToActivate(jingleSession.getRemote());
                activate.setFrom(jingleSession.getLocal());
                try {
                    jingleSession.getConnection().createStanzaCollectorAndSend(activate).nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not activate proxy.", e);
                    return;
                }

                LOGGER.log(Level.INFO, "Send candidate-activate.");
                JingleElement candidateActivate = transportManager().createCandidateActivated(
                        jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                        content.getSenders(), content.getCreator(), content.getName(), nominated.transport.getStreamId(),
                        nominated.candidate.getCandidateId());
                try {
                    jingleSession.getConnection().createStanzaCollectorAndSend(candidateActivate)
                            .nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not send candidate-activated", e);
                    return;
                }
            }

            LOGGER.log(Level.INFO, "Start transmission.");
            Socks5BytestreamSession bs = new Socks5BytestreamSession(nominated.socket, !isProxy);
            callback.onSessionInitiated(bs);

        }
        //Our choice
        else {
            LOGGER.log(Level.INFO, "Our choice, so their candidate was used.");
            boolean isProxy = nominated.candidate.getType() == JingleS5BTransportCandidateElement.Type.proxy;
            if (!isProxy) {
                LOGGER.log(Level.INFO, "Direct connection.");
                Socks5BytestreamSession bs = new Socks5BytestreamSession(nominated.socket, true);
                callback.onSessionInitiated(bs);
            } else {
                LOGGER.log(Level.INFO, "Our choice was their external proxy. wait for candidate-activate.");
            }
        }
    }

}
