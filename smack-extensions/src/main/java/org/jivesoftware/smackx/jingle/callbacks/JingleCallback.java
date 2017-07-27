package org.jivesoftware.smackx.jingle.callbacks;

import org.jivesoftware.smackx.jingle.JingleDescriptionController;

/**
 * Created by vanitas on 27.07.17.
 */
public interface JingleCallback<P extends JingleCallback.Parameters> {

    JingleDescriptionController accept(P parameters);

    void decline();

    class Parameters {

    }
}
