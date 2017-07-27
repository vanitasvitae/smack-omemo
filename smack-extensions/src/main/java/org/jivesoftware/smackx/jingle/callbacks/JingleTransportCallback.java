package org.jivesoftware.smackx.jingle.callbacks;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 27.07.17.
 */
public interface JingleTransportCallback {

    void onTransportReady(BytestreamSession bytestreamSession);

    void onTransportFailed(Exception e);
}
