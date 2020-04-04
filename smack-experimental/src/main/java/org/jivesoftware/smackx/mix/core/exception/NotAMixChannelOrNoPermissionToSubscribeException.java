package org.jivesoftware.smackx.mix.core.exception;

public class NotAMixChannelOrNoPermissionToSubscribeException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotAMixChannelOrNoPermissionToSubscribeException(String message) {
        super(message);
    }
}
