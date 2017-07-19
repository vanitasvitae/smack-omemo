package org.jivesoftware.smackx.jingle3.adapter;

import org.jivesoftware.smackx.jingle3.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle3.internal.Security;

/**
 * Created by vanitas on 18.07.17.
 */
public interface JingleSecurityAdapter<S extends Security<?>> {

    S securityFromElement(JingleContentSecurityElement element);

    String getNamespace();
}
