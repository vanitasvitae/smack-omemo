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
package org.jivesoftware.smackx.jingle_filetransfer;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.element.Jingle;

/**
 * AbstractJingleSession that implements basic handler methods that basically do nothing.
 */
public abstract class AbstractJingleSession implements JingleSessionHandler {

    protected final XMPPConnection connection;
    protected AbstractJingleSession state;

    public AbstractJingleSession(XMPPConnection connection) {
        this.connection = connection;
    }

    @Override
    public IQ handleJingleSessionRequest(Jingle jingle) {
        switch (jingle.getAction()) {
            case content_accept:
                return handleContentAccept(jingle);
            case content_add:
                return handleContentAdd(jingle);
            case content_modify:
                return handleContentModify(jingle);
            case content_reject:
                return handleContentReject(jingle);
            case content_remove:
                return handleContentRemove(jingle);
            case description_info:
                return handleDescriptionInfo(jingle);
            case session_info:
                return handleSessionInfo(jingle);
            case security_info:
                return handleSecurityInfo(jingle);
            case session_accept:
                return handleSessionAccept(jingle);
            case transport_accept:
                return handleTransportAccept(jingle);
            case transport_info:
                return handleTransportInfo(jingle);
            case session_initiate:
                return handleSessionInitiate(jingle);
            case transport_reject:
                return handleTransportReject(jingle);
            case session_terminate:
                return handleSessionTerminate(jingle);
            case transport_replace:
                return handleTransportReplace(jingle);
            default:
                return IQ.createResultIQ(jingle);
        }
    }

    protected IQ handleSessionInitiate(Jingle sessionInitiate) {
        return IQ.createResultIQ(sessionInitiate);
    }

    protected IQ handleSessionTerminate(Jingle sessionTerminate) {
        return IQ.createResultIQ(sessionTerminate);
    }

    protected IQ handleSessionInfo(Jingle sessionInfo) {
        return IQ.createResultIQ(sessionInfo);
    }

    protected IQ handleSessionAccept(Jingle sessionAccept) {
        return IQ.createResultIQ(sessionAccept);
    }

    protected IQ handleContentAdd(Jingle contentAdd) {
        return IQ.createResultIQ(contentAdd);
    }

    protected IQ handleContentAccept(Jingle contentAccept) {
        return IQ.createResultIQ(contentAccept);
    }

    protected IQ handleContentModify(Jingle contentModify) {
        return IQ.createResultIQ(contentModify);
    }

    protected IQ handleContentReject(Jingle contentReject) {
        return IQ.createResultIQ(contentReject);
    }

    protected IQ handleContentRemove(Jingle contentRemove) {
        return IQ.createResultIQ(contentRemove);
    }

    protected IQ handleDescriptionInfo(Jingle descriptionInfo) {
        return IQ.createResultIQ(descriptionInfo);
    }

    protected IQ handleSecurityInfo(Jingle securityInfo) {
        return IQ.createResultIQ(securityInfo);
    }

    protected IQ handleTransportAccept(Jingle transportAccept) {
        return IQ.createResultIQ(transportAccept);
    }

    protected IQ handleTransportInfo(Jingle transportInfo) {
        return IQ.createResultIQ(transportInfo);
    }

    protected IQ handleTransportReplace(Jingle transportReplace) {
        return IQ.createResultIQ(transportReplace);
    }

    protected IQ handleTransportReject(Jingle transportReject) {
        return IQ.createResultIQ(transportReject);
    }

    @Override
    public XMPPConnection getConnection() {
        return connection;
    }
}
