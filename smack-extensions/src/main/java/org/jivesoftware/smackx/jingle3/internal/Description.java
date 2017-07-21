package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.jingle3.element.JingleContentDescriptionElement;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Description<D extends JingleContentDescriptionElement> {

    private Content parent;

    public abstract D getElement();

    public void setParent(Content parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public Content getParent() {
        return parent;
    }
}
