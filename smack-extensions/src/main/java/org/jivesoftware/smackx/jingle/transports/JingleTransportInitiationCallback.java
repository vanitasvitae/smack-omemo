package org.jivesoftware.smackx.jingle.transports;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 20.06.17.
 */
public interface JingleTransportInitiationCallback {

    void onSessionInitiated(BytestreamSession bytestreamSession);


}
