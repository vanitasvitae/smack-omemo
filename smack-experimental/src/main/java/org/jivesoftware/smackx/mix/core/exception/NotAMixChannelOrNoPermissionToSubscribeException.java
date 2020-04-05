package org.jivesoftware.smackx.mix.core.exception;

/**
 * Exception that is thrown when the user tries to interact with an entity which is either not a MIX channel,
 * or is one but the user doesn't have the permission to subscribe to it.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0369.html#disco-channel-info">
 *     XEP-0369: §6.3 about discovering channel information</a>
 */
public class NotAMixChannelOrNoPermissionToSubscribeException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotAMixChannelOrNoPermissionToSubscribeException(String message) {
        super(message);
    }
}
