package org.jivesoftware.smackx.jingle3;

import org.jivesoftware.smackx.jingle3.internal.Content;

/**
 * Created by vanitas on 18.07.17.
 */
public abstract class Callback {

    private final Content content;

    public Callback(Content content) {
        this.content = content;
    }
}
