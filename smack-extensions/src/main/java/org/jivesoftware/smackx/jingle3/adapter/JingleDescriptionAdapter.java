package org.jivesoftware.smackx.jingle3.adapter;

import org.jivesoftware.smackx.jingle3.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle3.internal.Description;

/**
 * Created by vanitas on 18.07.17.
 */
public interface JingleDescriptionAdapter<D extends Description<?>> {

    D descriptionFromElement(JingleContentDescriptionElement element);

    String getNamespace();
}
