/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.jet.JingleEncryptedTransferManager;
import org.jivesoftware.smackx.jet.JingleEncryptionMethodManager;
import org.jivesoftware.smackx.jet.element.SecurityElement;

import org.xmlpull.v1.XmlPullParser;

/**
 * Created by vanitas on 13.07.17.
 */
public class SecurityProvider extends ExtensionElementProvider<SecurityElement> {
    private static final Logger LOGGER = Logger.getLogger(SecurityProvider.class.getName());

    @Override
    public SecurityElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String name = parser.getAttributeValue(JingleEncryptedTransferManager.NAMESPACE, SecurityElement.ATTR_NAME);
        String type = parser.getAttributeValue(JingleEncryptedTransferManager.NAMESPACE, SecurityElement.ATTR_TYPE);
        ExtensionElement child;

        Objects.requireNonNull(type);

        ExtensionElementProvider<ExtensionElement> encryptionElementProvider =
                JingleEncryptionMethodManager.getSecurityKeyTransportProvider(type);

        if (encryptionElementProvider != null) {
            child = encryptionElementProvider.parse(parser);
        } else {
            LOGGER.log(Level.WARNING, "Unknown child element in SecurityElement: " + type);
            return null;
        }

        return new SecurityElement(name, child);
    }
}
