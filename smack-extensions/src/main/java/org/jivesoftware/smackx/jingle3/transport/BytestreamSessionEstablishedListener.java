package org.jivesoftware.smackx.jingle3.transport;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 18.07.17.
 */
public interface BytestreamSessionEstablishedListener {

    void onBytestreamSessionEstablished(BytestreamSession session);

    void onBytestreamSessionFailed(Exception exception);

}
