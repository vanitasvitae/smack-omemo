package org.jivesoftware.smackx.jingle_encrypted_transfer;

import java.util.HashMap;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;

/**
 * Created by vanitas on 13.07.17.
 */
public class JingleEncryptionMethodManager {

    public static HashMap<String, ExtensionElementProvider<ExtensionElement>> securityKeyTransportProviders = new HashMap<>();

    private JingleEncryptionMethodManager() {
        // $(man true)
    }

    public static void registerSecurityKeyTransportProvider(String namespace, ExtensionElementProvider<ExtensionElement> provider) {
        securityKeyTransportProviders.put(namespace, provider);
    }

    public static ExtensionElementProvider<ExtensionElement> getSecurityKeyTransportProvider(String namespace) {
        return securityKeyTransportProviders.get(namespace);
    }
}
