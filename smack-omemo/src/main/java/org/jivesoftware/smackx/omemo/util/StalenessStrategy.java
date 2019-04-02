/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.omemo.util;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

/**
 * This interface can be extended in order to implement different strategies to determine the staleness of an
 * {@link OmemoDevice}. Stale devices SHOULD no longer be included in the list of recipients during message encryption.
 */
public interface StalenessStrategy {

    /**
     * Return true if the device is considered stale, false otherwise.
     *
     * @param userDevice our device
     * @param contactsDevice device of a contact
     * @return stale or not
     */
    boolean isStale(OmemoDevice userDevice, OmemoDevice contactsDevice);
}
