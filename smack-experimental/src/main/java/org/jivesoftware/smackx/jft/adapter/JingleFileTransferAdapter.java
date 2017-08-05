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
package org.jivesoftware.smackx.jft.adapter;

import java.util.List;

import org.jivesoftware.smackx.jft.component.JingleFileTransfer;
import org.jivesoftware.smackx.jft.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jft.component.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jft.element.JingleFileTransferElement;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;

/**
 * Created by vanitas on 28.07.17.
 */
public class JingleFileTransferAdapter implements JingleDescriptionAdapter<JingleFileTransfer> {

    @Override
    public JingleFileTransfer descriptionFromElement(JingleContentElement.Creator creator, JingleContentElement.Senders senders,
                                                     String contentName, String contentDisposition, JingleContentDescriptionElement element) {
        JingleFileTransferElement description = (JingleFileTransferElement) element;
        List<JingleContentDescriptionChildElement> childs = description.getJingleContentDescriptionChildren();
        assert childs.size() == 1;
        JingleFileTransferChildElement file = (JingleFileTransferChildElement) childs.get(0);

        if (senders == JingleContentElement.Senders.initiator) {
            return new JingleIncomingFileOffer(file);
        } else if (senders == JingleContentElement.Senders.responder) {
            return new JingleIncomingFileRequest(file);
        } else {
            throw new AssertionError("Senders attribute MUST be either initiator or responder. Is: " + senders);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }
}
