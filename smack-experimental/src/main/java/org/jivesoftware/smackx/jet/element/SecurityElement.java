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
package org.jivesoftware.smackx.jet.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jet.JingleEncryptedTransferManager;

/**
 * Created by vanitas on 13.07.17.
 */
public class SecurityElement implements ExtensionElement {
    public static final String ELEMENT = "security";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TYPE = "type";

    private final ExtensionElement child;
    private final String name;

    public SecurityElement(String name, ExtensionElement child) {
        this.name = name;
        this.child = child;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_NAME, name).attribute(ATTR_TYPE, child.getNamespace());
        xml.rightAngleBracket();
        xml.element(child);
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getNamespace() {
        return JingleEncryptedTransferManager.NAMESPACE;
    }
}
