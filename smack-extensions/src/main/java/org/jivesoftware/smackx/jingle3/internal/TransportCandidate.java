package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.jingle3.element.JingleContentTransportCandidateElement;

/**
 * Created by vanitas on 21.07.17.
 */
public abstract class TransportCandidate<E extends JingleContentTransportCandidateElement> {

    private Transport<?> parent;
    private int priority;

    public void setParent(Transport<?> transport) {
        if (parent != transport) {
            parent = transport;
        }
    }

    public Transport<?> getParent() {
        return parent;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public abstract E getElement();
}
