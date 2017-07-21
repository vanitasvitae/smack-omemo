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
package org.jivesoftware.smackx.jingle.transport.jingle_ibb;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.internal.JingleTransport;
import org.jivesoftware.smackx.jingle.internal.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.transport.BytestreamSessionEstablishedListener;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.internal.JingleSession;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.element.JingleIBBTransportElement;

/**
 * Jingle InBandBytestream Transport component.
 */
public class JingleIBBTransport extends JingleTransport<JingleIBBTransportElement> {

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    private final String streamId;
    private final Short blockSize;

    public JingleIBBTransport(String streamId, Short blockSize) {
        this.streamId = streamId;
        this.blockSize = blockSize;
    }

    public JingleIBBTransport() {
        this(StringUtils.randomString(10), JingleIBBTransportElement.DEFAULT_BLOCK_SIZE);
    }

    public Short getBlockSize() {
        return blockSize;
    }

    public String getSid() {
        return streamId;
    }

    @Override
    public JingleIBBTransportElement getElement() {
        return new JingleIBBTransportElement(streamId, blockSize);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void establishIncomingBytestreamSession(final BytestreamSessionEstablishedListener listener, final XMPPConnection connection) {
        final JingleSession session = getParent().getParent();
        InBandBytestreamManager.getByteStreamManager(connection)
                .addIncomingBytestreamListener(new BytestreamListener() {
                    @Override
                    public void incomingBytestreamRequest(BytestreamRequest request) {
                        if (request.getFrom().asFullJidIfPossible().equals(session.getPeer())
                                && request.getSessionID().equals(getSid())) {
                            BytestreamSession bytestreamSession;

                            try {
                                bytestreamSession = request.accept();
                            } catch (InterruptedException | SmackException | XMPPException.XMPPErrorException e) {
                                listener.onBytestreamSessionFailed(e);
                                return;
                            }
                            listener.onBytestreamSessionEstablished(bytestreamSession);
                        }
                    }
                });
    }

    @Override
    public void establishOutgoingBytestreamSession(BytestreamSessionEstablishedListener listener, XMPPConnection connection) {
        JingleSession session = getParent().getParent();
        InBandBytestreamManager inBandBytestreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        inBandBytestreamManager.setDefaultBlockSize(blockSize);
        try {
            BytestreamSession bytestreamSession = inBandBytestreamManager.establishSession(session.getPeer(), getSid());
            listener.onBytestreamSessionEstablished(bytestreamSession);
        } catch (SmackException.NoResponseException | XMPPException.XMPPErrorException | InterruptedException | SmackException.NotConnectedException e) {
            listener.onBytestreamSessionFailed(e);
        }
    }

    @Override
    public void addCandidate(JingleTransportCandidate<?> candidate) {
        // Sorry, we don't want any candidates.
    }

    @Override
    public void handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping) {
        // Nothing to do.
    }
}
