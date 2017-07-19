package org.jivesoftware.smackx.jingle3;

import org.jivesoftware.smackx.jingle3.callbacks.ContentAddCallback;
import org.jivesoftware.smackx.jingle3.element.JingleElement;
import org.jivesoftware.smackx.jingle3.internal.Content;

/**
 * Created by vanitas on 19.07.17.
 */
public interface JingleDescriptionManager {

    String getNamespace();

    JingleElement notifyContentListeners(Content content, ContentAddCallback callback);
}
