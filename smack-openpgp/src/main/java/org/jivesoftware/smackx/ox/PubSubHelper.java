/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.xdata.packet.DataForm;

public class PubSubHelper {

    public static void whitelist(LeafNode node)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        ConfigureForm old = node.getNodeConfiguration();
        if (old.getAccessModel() != AccessModel.whitelist) {
            ConfigureForm _new = new ConfigureForm(DataForm.Type.submit);
            _new.setAccessModel(AccessModel.whitelist);
            node.sendConfigurationForm(_new);
        }
    }

    public static void open(LeafNode node)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        ConfigureForm config = node.getNodeConfiguration();
        if (config.getAccessModel() != AccessModel.open) {
            config.setAccessModel(AccessModel.open);
            node.sendConfigurationForm(config);
        }
    }
}
