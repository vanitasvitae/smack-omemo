package org.jivesoftware.smackx.jingle_encrypted_transfer.provider;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.jingle_encrypted_transfer.JingleEncryptedTransferManager;
import org.jivesoftware.smackx.jingle_encrypted_transfer.JingleEncryptionMethodManager;
import org.jivesoftware.smackx.jingle_encrypted_transfer.element.SecurityElement;

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
