/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.mix.core.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.util.XmlStringBuilder;

public abstract class AbstractSubscriptionsModifyingElement {

    private final List<SubscribeElement> nodeSubscriptions;

    public AbstractSubscriptionsModifyingElement(List<SubscribeElement> nodeSubscriptions) {
        this.nodeSubscriptions = Collections.unmodifiableList(nodeSubscriptions);
    }

    public List<SubscribeElement> getNodeSubscriptions() {
        return nodeSubscriptions;
    }

    protected void appendSubscribeElementsToXml(XmlStringBuilder xml) {
        for (SubscribeElement subscribe : getNodeSubscriptions()) {
            xml.append(subscribe);
        }
    }
}
