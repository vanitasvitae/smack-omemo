package org.jivesoftware.smackx.jingle3;

import org.jivesoftware.smackx.jingle3.internal.Session;

/**
 * Created by vanitas on 19.07.17.
 */
public interface JingleSessionInitiateListener {
    void onJingleSessionInitiate(Session session);
}
