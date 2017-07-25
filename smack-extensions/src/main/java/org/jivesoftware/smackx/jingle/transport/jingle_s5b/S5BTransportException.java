package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import org.jivesoftware.smackx.jingle.exception.FailedTransportException;

/**
 * Created by vanitas on 25.07.17.
 */
public class S5BTransportException extends FailedTransportException {

    protected static final long serialVersionUID = 1L;

    private S5BTransportException(Throwable throwable) {
        super(throwable);
    }

    public static class CandidateError extends S5BTransportException {

        public CandidateError(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ProxyError extends S5BTransportException {

        public ProxyError(Throwable throwable) {
            super(throwable);
        }
    }
}
