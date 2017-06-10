package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.exception.JingleTransportFailureException;

/**
 * Created by vanitas on 10.06.17.
 */
public interface JingleTransportEstablishedCallback {
    void onSessionEstablished(BytestreamSession bytestreamSession);

    void onSessionFailure(JingleTransportFailureException reason);
}
