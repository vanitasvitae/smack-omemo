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
package org.jivesoftware.smackx.jingle_ibb;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;
import org.jivesoftware.smackx.jingle_ibb.element.JingleIBBTransport;

/**
 * JingleTransportHandler for InBandBytestreams.
 */
public class JingleIBBTransportHandler implements JingleTransportHandler<JingleIBBTransport> {

    private final JingleSessionHandler jingleSessionHandler;

    public JingleIBBTransportHandler(JingleSessionHandler sessionHandler) {
        this.jingleSessionHandler = sessionHandler;
    }

    @Override
    public void prepareSession() {

    }

    @Override
    public void establishOutgoingSession(JingleTransportEstablishedCallback callback) {
        InBandBytestreamSession session;


        try {
            session = InBandBytestreamManager.getByteStreamManager(getConnection())
                    .establishSession(jingleSessionHandler.getFullJidAndSessionId().getFullJid(), jingleSessionHandler.getFullJidAndSessionId().getSessionId());
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException e) {
            callback.onSessionFailure(new JingleTransportFailureException(e));
            return;
        }

        callback.onSessionEstablished(session);
    }

    @Override
    public void establishIncomingSession(final JingleTransportEstablishedCallback callback) {
        InBandBytestreamManager.getByteStreamManager(getConnection()).addIncomingBytestreamListener(new BytestreamListener() {
            @Override
            public void incomingBytestreamRequest(BytestreamRequest request) {
                if (request.getFrom().asFullJidIfPossible().equals(jingleSessionHandler.getFullJidAndSessionId().getFullJid())
                        && request.getSessionID().equals(jingleSessionHandler.getFullJidAndSessionId().getSessionId())) {
                    BytestreamSession session;

                    try {
                        session = request.accept();
                    } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                        callback.onSessionFailure(new JingleTransportFailureException(e));
                        return;
                    }
                    callback.onSessionEstablished(session);
                }
            }
        });
    }


    @Override
    public XMPPConnection getConnection() {
        return jingleSessionHandler.getConnection();
    }

    @Override
    public boolean onTransportInfoReceived(Jingle transportInfo) {
        return false;
    }
}
