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
 * Example {@link StalenessStrategy} which never rates OMEMO sessions as stale.
 *
 * Note: This should only be used for testing purposes.
 */
public class NeverStaleStalenessStrategy implements StalenessStrategy {
    @Override
    public boolean isStale(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        // Accept all sessions as non-stale
        return false;
    }
}
