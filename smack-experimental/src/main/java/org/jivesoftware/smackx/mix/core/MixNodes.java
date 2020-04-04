/**
 *
 * Copyright 2020 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.mix.core;

import org.jivesoftware.smackx.mix.core.element.SubscribeElement;
import org.jivesoftware.smackx.mix.presence.MixPresenceConstants;

public class MixNodes {
    public static final SubscribeElement NODE_MESSAGES = new SubscribeElement(MixCoreConstants.NODE_MESSAGES);
    public static final SubscribeElement NODE_PRESENCE = new SubscribeElement(MixPresenceConstants.NODE_PRESENCE);
    public static final SubscribeElement NODE_PARTICIPANTS = new SubscribeElement(MixCoreConstants.NODE_PARTICIPANTS);
    public static final SubscribeElement NODE_INFO = new SubscribeElement(MixCoreConstants.NODE_INFO);
}
