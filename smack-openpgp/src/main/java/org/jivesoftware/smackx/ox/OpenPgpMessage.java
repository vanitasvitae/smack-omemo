/**
 *
 * Copyright 2017 Florian Schmaus.
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

import java.io.IOException;

import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.provider.OpenPgpContentElementProvider;

import org.xmlpull.v1.XmlPullParserException;

/**
 * This class embodies a decrypted {@link OpenPgpElement}.
 */
public class OpenPgpMessage {

    public enum State {
        /**
         * Represents a {@link SigncryptElement}.
         */
        signcrypt,
        /**
         * Represents a {@link SignElement}.
         */
        sign,
        /**
         * Represents a {@link CryptElement}.
         */
        crypt,
        ;
    }

    private final String element;
    private State state;

    private OpenPgpContentElement openPgpContentElement;

    /**
     * Constructor.
     *
     * @param state state of the content element.
     * @param content XML representation of the decrypted {@link OpenPgpContentElement}.
     */
    public OpenPgpMessage(State state, String content) {
        this.state = state;
        this.element = content;
    }

    /**
     * Return the decrypted {@link OpenPgpContentElement} of this message.
     * To determine, whether the element is a {@link SignElement}, {@link CryptElement} or {@link SigncryptElement},
     * please consult {@link #getState()}.
     *
     * @return {@link OpenPgpContentElement}
     * @throws XmlPullParserException if the parser encounters an error.
     * @throws IOException if the parser encounters an error.
     */
    public OpenPgpContentElement getOpenPgpContentElement() throws XmlPullParserException, IOException {
        ensureOpenPgpContentElementSet();

        return openPgpContentElement;
    }

    private void ensureOpenPgpContentElementSet() throws XmlPullParserException, IOException {
        if (openPgpContentElement != null)
            return;

        openPgpContentElement = OpenPgpContentElementProvider.parseOpenPgpContentElement(element);
        if (openPgpContentElement == null) {
            return;
        }

        // Determine the state of the content element.
        if (openPgpContentElement instanceof SigncryptElement) {
            state = State.signcrypt;
        } else if (openPgpContentElement instanceof SignElement) {
            state = State.sign;
        } else if (openPgpContentElement instanceof CryptElement) {
            state = State.crypt;
        } else {
            throw new AssertionError("OpenPgpContentElement is neither a SignElement, " +
                    "CryptElement nor a SignCryptElement.");
        }
    }

    /**
     * Return the state of the message. This value determines, whether the message was a {@link SignElement},
     * {@link CryptElement} or {@link SigncryptElement}.
     *
     * @return state of the content element.
     * @throws IOException if the parser encounters an error.
     * @throws XmlPullParserException if the parser encounters and error.
     */
    public State getState() throws IOException, XmlPullParserException {
        ensureOpenPgpContentElementSet();
        return state;
    }
}
