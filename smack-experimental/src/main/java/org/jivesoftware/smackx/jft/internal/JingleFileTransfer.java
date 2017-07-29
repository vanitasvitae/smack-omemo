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
package org.jivesoftware.smackx.jft.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smackx.jft.controller.JingleFileTransferController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jft.internal.file.AbstractJingleFileTransferFile;
import org.jivesoftware.smackx.jft.listener.ProgressListener;
import org.jivesoftware.smackx.jingle.components.JingleDescription;

/**
 * Created by vanitas on 22.07.17.
 */
public abstract class JingleFileTransfer extends JingleDescription<JingleFileTransferElement> implements JingleFileTransferController {

    public static final String NAMESPACE_V5 = "urn:xmpp:jingle:apps:file-transfer:5";
    public static final String NAMESPACE = NAMESPACE_V5;

    public abstract boolean isOffer();
    public abstract boolean isRequest();

    protected State state;
    protected AbstractJingleFileTransferFile file;

    protected final List<ProgressListener> progressListeners = Collections.synchronizedList(new ArrayList<ProgressListener>());

    public JingleFileTransfer(AbstractJingleFileTransferFile file) {
        this.file = file;
    }

    @Override
    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
        //TODO: Notify new listener?
    }

    @Override
    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    public void notifyProgressListeners(float progress) {
        for (ProgressListener p : progressListeners) {
            p.progress(progress);
        }
    }

    public void notifyProgressListenersFinished() {
        for (ProgressListener p : progressListeners) {
            p.finished();
        }
    }

    public void notifyProgressListenersStarted() {
        for (ProgressListener p : progressListeners) {
            p.started();
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }

    @Override
    public JingleFileTransferElement getElement() {
        return new JingleFileTransferElement(file.getElement());
    }

    @Override
    public State getState() {
        return state;
    }
}
