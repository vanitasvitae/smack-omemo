package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.jingle3.element.JingleAction;

/**
 * Created by vanitas on 19.07.17.
 */
public abstract class PendingJingleAction {
    private final JingleAction action;
    private final Content affectedContent;

    public PendingJingleAction(JingleAction action, Content content) {
        this.action = action;
        this.affectedContent = content;
    }

    public JingleAction getAction() {
        return action;
    }

    public Content getAffectedContent() {
        return affectedContent;
    }

    public static class TransportReplace extends PendingJingleAction {
        private final Transport<?> newTransport;

        public TransportReplace(Content content, Transport<?> newTransport) {
            super(JingleAction.transport_replace, content);
            this.newTransport = newTransport;
        }

        public Transport<?> getNewTransport() {
            return newTransport;
        }
    }
}
