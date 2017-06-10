package org.jivesoftware.smackx.jingle.exception;

/**
 * Created by vanitas on 10.06.17.
 */
public class JingleTransportFailureException extends Exception {

    private static final long serialVersionUID = 1L;

    public JingleTransportFailureException(Throwable wrapped) {
        super(wrapped);
    }
}
