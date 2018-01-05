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
package org.jivesoftware.smackx.omemo;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.BODY_OMEMO_HINT;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

public class OmemoMessage {

    private final OmemoElement element;
    private final byte[] messageKey, iv;

    OmemoMessage(OmemoElement element, byte[] key, byte[] iv) {
        this.element = element;
        this.messageKey = key;
        this.iv = iv;
    }

    public OmemoElement getElement() {
        return element;
    }

    public byte[] getKey() {
        return messageKey.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public static class Sent extends OmemoMessage {
        private final ArrayList<OmemoDevice> intendedDevices = new ArrayList<>();
        private final HashMap<OmemoDevice, Throwable> skippedDevices = new HashMap<>();

        Sent(OmemoElement element, byte[] key, byte[] iv, List<OmemoDevice> intendedDevices, HashMap<OmemoDevice, Throwable> skippedDevices) {
            super(element, key, iv);
            this.intendedDevices.addAll(intendedDevices);
            this.skippedDevices.putAll(skippedDevices);
        }

        public ArrayList<OmemoDevice> getIntendedDevices() {
            return intendedDevices;
        }

        public HashMap<OmemoDevice, Throwable> getSkippedDevices() {
            return skippedDevices;
        }

        public boolean isMissingRecipients() {
            return !getSkippedDevices().isEmpty();
        }

        public Message asMessage() {

            Message messageStanza = new Message();
            messageStanza.addExtension(getElement());

            if (OmemoConfiguration.getAddOmemoHintBody()) {
                messageStanza.setBody(BODY_OMEMO_HINT);
            }

            StoreHint.set(messageStanza);
            messageStanza.addExtension(new ExplicitMessageEncryptionElement(OMEMO_NAMESPACE_V_AXOLOTL, OMEMO));

            return messageStanza;
        }
    }

    public static class Received extends OmemoMessage {
        private final String message;
        private final OmemoFingerprint sendersFingerprint;
        private final OmemoDevice senderDevice;
        private final CARBON carbon;
        private final boolean preKeyMessage;

        Received(OmemoElement element, byte[] key, byte[] iv, String message, OmemoFingerprint sendersFingerprint, OmemoDevice senderDevice, CARBON carbon, boolean preKeyMessage) {
            super(element, key, iv);
            this.message = message;
            this.sendersFingerprint = sendersFingerprint;
            this.senderDevice = senderDevice;
            this.carbon = carbon;
            this.preKeyMessage = preKeyMessage;
        }

        public String getMessage() {
            return message;
        }

        public OmemoFingerprint getSendersFingerprint() {
            return sendersFingerprint;
        }

        public OmemoDevice getSenderDevice() {
            return senderDevice;
        }

        /**
         * Return the carbon type.
         *
         * @return carbon type
         */
        public CARBON getCarbon() {
            return carbon;
        }

        boolean isPreKeyMessage() {
            return preKeyMessage;
        }
    }

    /**
     * Types of Carbon Messages.
     */
    public enum CARBON {
        NONE,   //No carbon
        SENT,   //Sent carbon
        RECV    //Received Carbon
    }
}
