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

import java.util.HashSet;

import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferSession;

/**
 * Handler that provides some control over the JingleFileOffer session.
 */
public class SendFileHandler {

    private final JingleFileTransferSession session;
    private final HashSet<FinishedListener> finishedListeners = new HashSet<>();
    private final HashSet<AcceptedListener> acceptedListeners = new HashSet<>();

    public SendFileHandler(JingleFileTransferSession session) {
        this.session = session;
    }

    /**
     * Cancels the current file transfer.
     */
    public void cancel() {

    }

    /**
     * Returns true, if the file transfer is finished.
     * @return true if transfer finished.
     */
    public boolean isFinished() {
        return false;
    }

    /**
     * Add a new FinishedListener.
     * @param listener listener
     */
    public void addFinishedListener(FinishedListener listener) {
        finishedListeners.add(listener);
    }

    /**
     * Add a new AcceptedListener.
     * @param listener listener
     */
    public void addAcceptedListener(AcceptedListener listener) {
        acceptedListeners.add(listener);
    }

    /**
     * Notify all registered FinishedListeners that the file transfer has finished.
     */
    void notifyFinishedListeners() {
        for (FinishedListener f : finishedListeners) {
            f.onFinished();
        }
    }

    /**
     * Notify all registered AcceptedListeners that the file transfer session has been accepted by the remote user.
     */
    void notifyAcceptedListeners() {
        for (AcceptedListener a : acceptedListeners) {
            a.onAccepted();
        }
    }

    /**
     * A FinishedListener will be notified by the SendFileHandler when the corresponding file transfer is finished.
     */
    public interface FinishedListener {
        void onFinished();
    }

    /**
     * An AcceptedListener will be notified by the SendFileHandler when the corresponding pending session has been
     * accepted by the remote user.
     */
    public interface AcceptedListener {
        void onAccepted();
    }
}
