package org.jivesoftware.smackx.jingle.transports;

/**
 * Created by vanitas on 25.06.17.
 */
public abstract class JingleTransportInitiationException extends Exception {
    private static final long serialVersionUID = 1L;


    public static class ProxyError extends JingleTransportInitiationException {
        private static final long serialVersionUID = 1L;
    }

    public static class CandidateError extends JingleTransportInitiationException {
        private static final long serialVersionUID = 1L;
    }
}
