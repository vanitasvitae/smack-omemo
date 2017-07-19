package org.jivesoftware.smackx.jingle3.exception;

/**
 * Created by vanitas on 18.07.17.
 */
public class UnsupportedSecurityException extends Exception {
    private static final long serialVersionUID = 1L;

    private final String namespace;

    public UnsupportedSecurityException(String namespace) {
        super("Security with namespace " + namespace + " not supported.");
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
}
