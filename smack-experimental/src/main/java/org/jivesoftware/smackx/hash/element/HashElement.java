/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.hash.element;


import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.hash.HashUtil;

import static org.jivesoftware.smack.util.Objects.requireNonNull;

/**
 * Represent a hash element.
 *
 * @author Paul Schaub
 */
public class HashElement implements ExtensionElement {



    public static final String ELEMENT = "hash";
    public static final String NAMESPACE = "urn:xmpp:hashes:2";
    public static final String ALGO = "algo";

    private final HashUtil.ALGORITHM algorithm;
    private final byte[] hash;
    private final String hashB64;

    public HashElement(HashUtil.ALGORITHM type, byte[] hash) {
        this.algorithm = requireNonNull(type);
        this.hash = requireNonNull(hash);
        hashB64 = Base64.encodeToString(hash);
    }

    public HashElement(HashUtil.ALGORITHM type, String hashB64) {
        this.algorithm = type;
        this.hash = Base64.decode(hashB64);
        this.hashB64 = hashB64;
    }

    public static HashElement fromData(HashUtil.ALGORITHM algorithm, byte[] data) {
        return new HashElement(algorithm, HashUtil.hash(algorithm, data));
    }

    public HashUtil.ALGORITHM getAlgorithm() {
        return algorithm;
    }

    public byte[] getHash() {
        return hash;
    }

    public String getHashB64() {
        return hashB64;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this);
        sb.attribute(ALGO, algorithm.toString());
        sb.rightAngleBracket();
        sb.append(hashB64);
        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }
}
