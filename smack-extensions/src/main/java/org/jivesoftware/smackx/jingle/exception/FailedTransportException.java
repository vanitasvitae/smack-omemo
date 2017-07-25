package org.jivesoftware.smackx.jingle.exception;

/**
 * Created by vanitas on 25.07.17.
 */
public class FailedTransportException extends Exception {

    protected static final long serialVersionUID = 1L;

    public FailedTransportException(Throwable throwable) {
        super(throwable);
    }
}
