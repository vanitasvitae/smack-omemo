package org.jivesoftware.smackx.mix.core;

import org.jivesoftware.smackx.mix.core.element.SubscribeElement;

public class MixNodes {
    public static final SubscribeElement NODE_MESSAGES = new SubscribeElement(MixCoreConstants.NODE_MESSAGES);
    public static final SubscribeElement NODE_PRESENCE = new SubscribeElement(MixCoreConstants.NODE_PRESENCE);
    public static final SubscribeElement NODE_PARTICIPANTS = new SubscribeElement(MixCoreConstants.NODE_PARTICIPANTS);
    public static final SubscribeElement NODE_INFO = new SubscribeElement(MixCoreConstants.NODE_INFO);
}
