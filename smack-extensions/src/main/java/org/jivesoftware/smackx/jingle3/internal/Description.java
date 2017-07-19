package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.jingle3.element.JingleContentDescriptionElement;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Description<D extends JingleContentDescriptionElement> {

    public abstract D getElement();
}
