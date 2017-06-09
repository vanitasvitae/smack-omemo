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
package org.jivesoftware.smackx.jingle_filetransfer.listener;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.callback.JingleFileTransferCallback;

/**
 * Listener for incoming file transfers.
 */
public interface IncomingJingleFileTransferListener {

    void onIncomingJingleFileTransfer(Jingle jingle, JingleFileTransferCallback callback);
}
