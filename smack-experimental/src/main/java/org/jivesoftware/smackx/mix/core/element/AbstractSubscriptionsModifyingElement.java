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
