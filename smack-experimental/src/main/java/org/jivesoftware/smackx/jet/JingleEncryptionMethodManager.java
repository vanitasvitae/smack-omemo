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
package org.jivesoftware.smackx.jet;

import java.util.HashMap;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;

/**
 * Created by vanitas on 13.07.17.
 */
public final class JingleEncryptionMethodManager {

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
