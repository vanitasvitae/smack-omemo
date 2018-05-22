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

import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.provider.OpenPgpContentElementProvider;

import org.xmlpull.v1.XmlPullParserException;

public class OpenPgpMessage {

    public enum State {
        signcrypt,
        sign,
        crypt,
        ;
    }

    private final String element;
    private State state;

    private OpenPgpContentElement openPgpContentElement;

    public OpenPgpMessage(State state, String content) {
        this.state = state;
        this.element = content;
    }

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

        if (openPgpContentElement instanceof SigncryptElement) {
            state = State.signcrypt;
        } else if (openPgpContentElement instanceof SignElement) {
            state = State.sign;
        } else {
            state = State.crypt;
        }
    }

    public State getState() throws IOException, XmlPullParserException {
        ensureOpenPgpContentElementSet();
        return state;
    }
}
