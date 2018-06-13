/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.exception;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

import org.jxmpp.jid.BareJid;

/**
 * Exception that gets thrown when an operation is missing an OpenPGP public key.
 */
public class MissingOpenPgpPublicKeyException extends Exception {

    private static final long serialVersionUID = 1L;

    private BareJid user;
    private OpenPgpV4Fingerprint fingerprint;

    /**
     * Create a new {@link MissingOpenPgpPublicKeyException}.
     *
     * @param owner {@link BareJid} of the keys owner.
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the missing key.
     */
    public MissingOpenPgpPublicKeyException(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        super("Missing public key " + fingerprint.toString() + " for user " + owner + ".");
        this.user = owner;
        this.fingerprint = fingerprint;
    }

    public MissingOpenPgpPublicKeyException(Throwable e) {

    }

    /**
     * Return the {@link BareJid} of the owner of the missing key.
     *
     * @return owner of missing key.
     */
    public BareJid getUser() {
        return user;
    }

    /**
     * Return the fingerprint of the missing key.
     *
     * @return {@link OpenPgpV4Fingerprint} of the missing key.
     */
    public OpenPgpV4Fingerprint getFingerprint() {
        return fingerprint;
    }
}
