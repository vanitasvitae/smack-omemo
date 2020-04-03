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

public class MixCoreConstants {

    public static final String URN_XMPP_MIX = "urn:xmpp:mix";
    public static final String MIX_NODES = URN_XMPP_MIX + ":nodes";

    public static final String NAMESPACE_CORE_BARE = "urn:xmpp:mix:core";
    public static final String NAMESPACE_CORE_1 = NAMESPACE_CORE_BARE + ":1";

    /**
     * This indicates support of MIX, and is returned by all MIX services.
     */
    public static final String NAMESPACE_CORE = NAMESPACE_CORE_1;

    public static final String FEATURE_CORE_1 = NAMESPACE_CORE_1;

    public static final String FEATURE_SEARCHABLE_1 = FEATURE_CORE_1 + "#searchable";

    public static final String FEATURE_CREATE_CHANNEL_1 = FEATURE_CORE_1 + "#create-channel";

    /**
     * For distributing messages to the channel. Each item of this node
     * will contain a message sent to the channel.
     */
    public static final String NODE_MESSAGES = MIX_NODES + ":messages";

    /**
     * For storing the list of participants and the associated nick.
     * Channel participants are added when they join the channel and
     * removed when they leave.
     */
    public static final String NODE_PARTICIPANTS = MIX_NODES + ":participants";

    /**
     * For storing general channel information, such as description.
     */
    public static final String NODE_INFO = MIX_NODES + ":info";
}
