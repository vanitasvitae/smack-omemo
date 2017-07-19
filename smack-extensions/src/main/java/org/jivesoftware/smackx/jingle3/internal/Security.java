package org.jivesoftware.smackx.jingle3.internal;

import org.jivesoftware.smackx.jingle3.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.element.JingleContentSecurityInfoElement;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Security<D extends JingleContentSecurityElement> {

    public abstract D getElement();

    public abstract JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element);
}
