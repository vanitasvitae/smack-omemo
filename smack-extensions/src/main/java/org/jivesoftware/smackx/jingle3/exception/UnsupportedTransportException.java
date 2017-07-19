package org.jivesoftware.smackx.jingle3.exception;

/**
 * Created by vanitas on 18.07.17.
 */
public class UnsupportedTransportException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String namespace;

    public UnsupportedTransportException(String namespace) {
        super("Transport with namespace " + namespace + " not supported.");
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
}
