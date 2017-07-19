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
package org.jivesoftware.smackx.jingle_filetransfer.handler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle3.element.JingleReasonElement;

/**
 * Handler that provides some control over the JingleFileOffer session.
 */
public interface FileTransferHandler {

    /**
     * Cancels the current file transfer.
     */
    void cancel() throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException;

    /**
     * Returns true, if the file transfer is ended.
     * @return true if transfer ended.
     */
    boolean isFinished();

    /**
     * Returns true, if the file transfer is started.
     * @return true if started.
     */
    boolean isStarted();

    /**
     * Add a new FinishedListener.
     * @param listener listener
     */
    void addEndedListener(EndedListener listener);

    /**
     * Add a new AcceptedListener.
     * @param listener listener
     */
    void addStartedListener(StartedListener listener);

    /**
     * Notify all registered FinishedListeners that the file transfer has ended.
     */
    void notifyEndedListeners(JingleReasonElement.Reason reason);

    /**
     * Notify all registered AcceptedListeners that the file transfer session has been accepted by the remote user.
     */
    void notifyStartedListeners();

    /**
     * A FinishedListener will be notified by the SendFileHandler when the corresponding file transfer is ended.
     */
    interface EndedListener {
        void onEnded(JingleReasonElement.Reason reason);
    }

    /**
     * An AcceptedListener will be notified by the SendFileHandler when the corresponding pending session has been
     * accepted by the remote user.
     */
    interface StartedListener {
        void onStarted();
    }
}
