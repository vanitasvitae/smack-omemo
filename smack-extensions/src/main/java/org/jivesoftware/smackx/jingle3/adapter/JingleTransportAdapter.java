package org.jivesoftware.smackx.jingle3.adapter;

import org.jivesoftware.smackx.jingle3.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle3.internal.Transport;

/**
 * Created by vanitas on 18.07.17.
 */
public interface JingleTransportAdapter<T extends Transport<?>> {

    T transportFromElement(JingleContentTransportElement element);

    String getNamespace();
}