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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

/**
 * This {@link StalenessStrategy} determines the staleness of a device by utilizing the ChainKey index of the sessions
 * sending chain. If the chain key index exceeds a certain value, the device is considered to be stale.
 * This means, that once we sent too many messages to the device without receiving an answer, we consider the device
 * stale.
 */
public class ChainKeyIndexStalenessStrategy<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> implements StalenessStrategy {

    private static final Logger LOGGER = Logger.getLogger(ChainKeyIndexStalenessStrategy.class.getName());

    private final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store;
    private final int maxChainKeyIndex;

    /**
     * Create a new {@link ChainKeyIndexStalenessStrategy}.
     *
     * @param store {@link OmemoStore} from which we can read the devices session.
     * @param maxChainKeyIndex the maximum value of messages we are allowed to send to the device unanswered before
     *                         we consider it stale.
     */
    public ChainKeyIndexStalenessStrategy(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store, int maxChainKeyIndex) {
        this.store = store;
        this.maxChainKeyIndex = maxChainKeyIndex;
    }

    @Override
    public boolean isStale(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        T_Sess sessionRecord = store.loadRawSession(userDevice, contactsDevice);
        if (sessionRecord == null) {
            return false;
        }

        int chainLength = store.keyUtil().lengthOfSessionSendingChain(sessionRecord);
        LOGGER.log(Level.FINE, "Staleness factor of OMEMO session between "
                + userDevice + " and " + contactsDevice + ": " + chainLength + "/" + maxChainKeyIndex);
        return chainLength >= maxChainKeyIndex;
    }
}
