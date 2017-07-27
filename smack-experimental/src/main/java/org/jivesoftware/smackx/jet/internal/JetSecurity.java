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
package org.jivesoftware.smackx.jet.internal;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.components.JingleSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Created by vanitas on 22.07.17.
 */
public class JetSecurity extends JingleSecurity<JetSecurityElement> {

    public static final String NAMESPACE_V0 = "urn:xmpp:jingle:jet:0";
    public static final String NAMESPACE = NAMESPACE_V0;

    private ExtensionElement child;

    @Override
    public JetSecurityElement getElement() {
        return new JetSecurityElement(getParent().getName(), child);
    }

    @Override
    public JingleElement handleSecurityInfo(JingleContentSecurityInfoElement element, JingleElement wrapping) {
        return null;
    }
}
