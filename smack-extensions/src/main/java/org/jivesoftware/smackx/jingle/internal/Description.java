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
package org.jivesoftware.smackx.jingle.internal;

import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;

/**
 * Class that represents a contents description component.
 */
public abstract class Description<D extends JingleContentDescriptionElement> {

    private Content parent;

    public abstract D getElement();

    public void setParent(Content parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public Content getParent() {
        return parent;
    }
}
