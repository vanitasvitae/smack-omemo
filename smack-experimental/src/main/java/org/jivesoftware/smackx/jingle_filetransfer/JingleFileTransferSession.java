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
package org.jivesoftware.smackx.jingle_filetransfer;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jxmpp.jid.Jid;

/**
 * Class that represents a jingle session in the context of Jingle File Transfer.
 */
public class JingleFileTransferSession extends JingleSession {

    public JingleFileTransferSession(Jid initiator, Jid responder, String sid) {
        super(initiator, responder, sid);
    }

    /**
     * A user might choose to abort all active transfers.
     * @return Jingle IQ that will abort all active transfers of this session.
     */
    IQ abortAllActiveFileTransfers() {
        Jingle.Builder builder = Jingle.getBuilder();
        builder.setResponder(getResponder().asFullJidOrThrow());
        builder.setInitiator(getInitiator().asFullJidOrThrow());
        builder.setAction(JingleAction.session_terminate);
        builder.setSessionId(getSid());
        builder.setReason(JingleReason.Reason.cancel);
        return builder.build();
    }

    /**
     * A user might want to abort the transfer of a single file.
     * @param content content which's transfer will be aborted.
     * @return Jingle IQ that will abort the transfer of the given content.
     */
    IQ abortSingleFileTransfer(JingleContent content) {
        Jingle.Builder builder = Jingle.getBuilder();
        builder.setResponder(getResponder().asFullJidOrThrow());
        builder.setInitiator(getInitiator().asFullJidOrThrow());
        builder.setAction(JingleAction.content_remove);
        builder.setSessionId(getSid());
        builder.addJingleContent(content);
        builder.setReason(JingleReason.Reason.cancel);
        return builder.build();
    }

    /**
     * Successfully end session after all files have been transferred.
     * @return Jingle IQ that will end the session.
     */
    IQ endSession() {
        Jingle.Builder builder = Jingle.getBuilder();
        builder.setResponder(getResponder().asFullJidOrThrow());
        builder.setInitiator(getInitiator().asFullJidOrThrow());
        builder.setAction(JingleAction.session_terminate);
        builder.setSessionId(getSid());
        builder.setReason(JingleReason.Reason.success);
        return builder.build();
    }
}
