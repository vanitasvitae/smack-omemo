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
package org.jivesoftware.smackx.ox;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;
import org.jivesoftware.smackx.hints.element.StoreHint;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;
import org.jivesoftware.smackx.ox.util.DecryptedBytesAndMetadata;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParserException;

public class OpenPgpContact {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpContact.class.getName());

    private final BareJid jid;
    protected final OpenPgpProvider cryptoProvider;
    private final XMPPConnection connection;

    private Map<OpenPgpV4Fingerprint, Date> announcedKeys = null;
    private Map<OpenPgpV4Fingerprint, Date> availableKeys = null;
    private final Map<OpenPgpV4Fingerprint, Throwable> unfetchableKeys = new HashMap<>();

    /**
     * Create a OpenPgpContact.
     *
     * @param cryptoProvider {@link OpenPgpProvider}
     * @param jid {@link BareJid} of the contact
     * @param connection our authenticated {@link XMPPConnection}
     */
    public OpenPgpContact(OpenPgpProvider cryptoProvider,
                          BareJid jid,
                          XMPPConnection connection) {
        this.jid = jid;
        this.cryptoProvider = cryptoProvider;
        this.connection = connection;
    }

    /**
     * Return the {@link BareJid} of the contact.
     *
     * @return jid
     */
    public BareJid getJid() {
        return jid;
    }

    /**
     * Return a {@link Map} of the announced keys of the contact and their last update dates.
     *
     * @return announced keys
     */
    public Map<OpenPgpV4Fingerprint, Date> getAnnouncedKeys() {
        if (announcedKeys == null) {
            announcedKeys = cryptoProvider.getStore().getAnnouncedKeysFingerprints(getJid());
        }
        return announcedKeys;
    }

    /**
     * Return a {@link Map} of all locally available keys of the contact.
     *
     * Note: This list might contain keys which are no longer associated to the contact.
     * For encryption please use {@link #getActiveKeys()} instead.
     *
     * @return available keys
     * @throws SmackOpenPgpException if we cannot read the locally available keys for some reason
     */
    public Map<OpenPgpV4Fingerprint, Date> getAvailableKeys() throws SmackOpenPgpException {
        if (availableKeys == null) {
            availableKeys = cryptoProvider.getStore().getAvailableKeysFingerprints(getJid());
        }
        return availableKeys;
    }

    /**
     * Return a {@link Map} of all the keys which cannot be fetched and the reason why this is.
     *
     * @return unfetched keys
     */
    public Map<OpenPgpV4Fingerprint, Throwable> getUnfetchableKeys() {
        return unfetchableKeys;
    }

    /**
     * Return a {@link Set} of all active keys of this contact.
     * Active keys are keys which are announced, not revoked or otherwise invalidated and locally available.
     *
     * @return active keys.
     * @throws SmackOpenPgpException if we cannot access the keys of the contact.
     */
    public Set<OpenPgpV4Fingerprint> getActiveKeys() throws SmackOpenPgpException {
        Set<OpenPgpV4Fingerprint> fingerprints = getAvailableKeys().keySet();
        fingerprints.retainAll(getAnnouncedKeys().keySet());
        return fingerprints;
    }

    /**
     * Fetch the metadata node to get a {@link PublicKeysListElement} and update any missing or outdated keys.
     *
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     * @throws SmackOpenPgpException
     */
    public void updateKeys()
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException, SmackOpenPgpException {
        updateKeys(PubSubDelegate.fetchPubkeysList(connection, getJid()));
    }

    /**
     * Update any missing or outdated keys based on the given {@link PublicKeysListElement}.
     *
     * @param metadata
     * @throws SmackOpenPgpException
     */
    public void updateKeys(PublicKeysListElement metadata)
            throws SmackOpenPgpException {
        storePublishedDevices(metadata);
        this.availableKeys = getAvailableKeys();

        for (OpenPgpV4Fingerprint fingerprint : announcedKeys.keySet()) {
            Date announcedDate = announcedKeys.get(fingerprint);
            Date availableDate = availableKeys.get(fingerprint);

            if (availableDate == null || availableDate.before(announcedDate)) {
                try {
                    updateKey(fingerprint);
                    unfetchableKeys.remove(fingerprint);
                } catch (IOException | XMPPException.XMPPErrorException | SmackException | InterruptedException |
                        SmackOpenPgpException | MissingUserIdOnKeyException | NullPointerException e) {
                    LOGGER.log(Level.WARNING, "Could not update key " + fingerprint + " of " +getJid());
                    unfetchableKeys.put(fingerprint, e);
                }
            }
        }
    }

    /**
     * Update the key identified by the {@code fingerprint}.
     *
     * @param fingerprint fingerprint of the key
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     * @throws IOException
     * @throws MissingUserIdOnKeyException
     * @throws SmackOpenPgpException
     */
    public void updateKey(OpenPgpV4Fingerprint fingerprint)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException, IOException,
            MissingUserIdOnKeyException, SmackOpenPgpException {
        PubkeyElement pubkeyElement = PubSubDelegate.fetchPubkey(connection, getJid(), fingerprint);
        if (pubkeyElement == null) {
            throw new NullPointerException("Fetched pubkeyElement for key " + fingerprint + " of " + getJid() + " is null.");
        }

        byte[] base64 = pubkeyElement.getDataElement().getB64Data();
        OpenPgpV4Fingerprint imported = importPublicKey(Base64.decode(base64));

        if (!fingerprint.equals(imported)) {
            // Not sure, if this can/should happen. Lets be safe and throw, even if its too late at this point.
            throw new AssertionError("Fingerprint of imported key differs from expected fingerprint. " +
                    "Expected: " + fingerprint + " Imported: " + imported);
        }
    }

    /**
     * Import a public key.
     *
     * @param data OpenPgp keys byte representation.
     * @return the fingerprint of the imported key.
     * @throws SmackOpenPgpException
     * @throws MissingUserIdOnKeyException
     * @throws IOException
     */
    private OpenPgpV4Fingerprint importPublicKey(byte[] data)
            throws SmackOpenPgpException, MissingUserIdOnKeyException, IOException {
        OpenPgpV4Fingerprint fingerprint = cryptoProvider.importPublicKey(getJid(), data);
        availableKeys.put(fingerprint, new Date());
        return fingerprint;
    }

    /**
     * Process an incoming {@link PublicKeysListElement} and store the contained {@link OpenPgpV4Fingerprint}s and
     * their publication dates in local storage.
     *
     * @param element publicKeysListElement
     * @return {@link Map} with the contents of the element
     */
    public Map<OpenPgpV4Fingerprint, Date> storePublishedDevices(PublicKeysListElement element) {
        Map<OpenPgpV4Fingerprint, Date> announcedKeys = new HashMap<>();

        for (OpenPgpV4Fingerprint f : element.getMetadata().keySet()) {
            PublicKeysListElement.PubkeyMetadataElement meta = element.getMetadata().get(f);
            announcedKeys.put(meta.getV4Fingerprint(), meta.getDate());
        }

        if (!announcedKeys.equals(this.announcedKeys)) {
            cryptoProvider.getStore().setAnnouncedKeysFingerprints(getJid(), announcedKeys);
            this.announcedKeys = announcedKeys;
        }
        return announcedKeys;
    }

    /**
     * Get all keys to which a message sent to the contact would be encrypted to.
     * Those are all active keys of the contact as well as our own active keys.
     *
     * @return encryption keys
     * @throws SmackOpenPgpException if we cannot read the contacts keys or our own keys.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     */
    private MultiMap<BareJid, OpenPgpV4Fingerprint> getEncryptionKeys()
            throws SmackOpenPgpException, SmackException.NotLoggedInException {
        OpenPgpSelf self = getSelf();

        Set<OpenPgpV4Fingerprint> contactsKeys = getActiveKeys();
        Set<OpenPgpV4Fingerprint> ourKeys = self.getActiveKeys();

        MultiMap<BareJid, OpenPgpV4Fingerprint> recipientsKeys = new MultiMap<>();
        for (OpenPgpV4Fingerprint fingerprint : contactsKeys) {
            recipientsKeys.put(getJid(), fingerprint);
        }

        for (OpenPgpV4Fingerprint fingerprint : ourKeys) {
            recipientsKeys.put(self.getJid(), fingerprint);
        }

        return recipientsKeys;
    }

    /**
     * Get our OpenPgpSelf.
     * The {@link OpenPgpSelf} contains all our fingerprints.
     *
     * @return openPgpSelf
     * @throws SmackException.NotLoggedInException
     */
    private OpenPgpSelf getSelf() throws SmackException.NotLoggedInException {
        return OpenPgpManager.getInstanceFor(connection).getOpenPgpSelf();
    }

    /**
     * Encrypt a message to a contact and ourselves and sign it with our signing key.
     * The payload will be wrapped in a {@link SigncryptElement} before encryption.
     *
     * @param payload the payload we want to encrypt.
     * @return {@link OpenPgpElement} containing the encrypted, signed {@link SigncryptElement}.
     * @throws IOException IO is dangerous
     * @throws SmackOpenPgpException OpenPgp is brittle
     * @throws MissingOpenPgpKeyPairException if we cannot read our signing key pair
     * @throws SmackException.NotLoggedInException if we are not logged in.
     */
    public OpenPgpElement encryptAndSign(List<ExtensionElement> payload)
            throws IOException, SmackOpenPgpException, MissingOpenPgpKeyPairException,
            SmackException.NotLoggedInException {

        OpenPgpSelf self = OpenPgpManager.getInstanceFor(connection).getOpenPgpSelf();

        SigncryptElement preparedPayload = new SigncryptElement(
                Collections.<Jid>singleton(getJid()),
                payload);

        byte[] encryptedBytes;

        // Encrypt the payload
        try {
            encryptedBytes = cryptoProvider.signAndEncrypt(
                    preparedPayload,
                    self.getSigningKey(),
                    getEncryptionKeys());
        } catch (MissingOpenPgpPublicKeyException e) {
            throw new AssertionError("Missing OpenPGP public key, even though this should not happen here.", e);
        }

        return new OpenPgpElement(Base64.encodeToString(encryptedBytes));
    }

    /**
     * Create a signed and encrypted {@link SigncryptElement} containing the {@code payload} and append it as a
     * {@link OpenPgpElement} to the {@code message}.
     *
     * @param message {@link Message} which will transport the payload.
     * @param payload payload that will be encrypted and signed.
     * @throws IOException IO is dangerous.
     * @throws SmackOpenPgpException OpenPGP is brittle.
     * @throws MissingOpenPgpKeyPairException if we cannot access our signing key.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     */
    public void addSignedEncryptedPayloadTo(Message message, List<ExtensionElement> payload)
            throws IOException, SmackOpenPgpException, MissingOpenPgpKeyPairException,
            SmackException.NotLoggedInException {

        // Add encrypted payload to message
        OpenPgpElement encryptedPayload = encryptAndSign(payload);
        message.addExtension(encryptedPayload);

        // Add additional information to the message
        if (!ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0)) {
            message.addExtension(new ExplicitMessageEncryptionElement(
                    ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        }
        StoreHint.set(message);
        message.setBody("This message is encrypted using XEP-0374: OpenPGP for XMPP: Instant Messaging.");

    }

    /**
     * Process an incoming {@link OpenPgpElement} and return the decrypted and verified {@link OpenPgpContentElement}.
     *
     * @param element possibly encrypted, possibly signed {@link OpenPgpElement}.
     * @return decrypted {@link OpenPgpContentElement}
     * @throws XmlPullParserException if the decrypted message does not represent valid XML.
     * @throws MissingOpenPgpKeyPairException if we are missing the public key counterpart of the key that signed the message.
     * @throws SmackOpenPgpException if the message cannot be decrypted and or verified.
     * @throws IOException IO is dangerous
     */
    public OpenPgpContentElement receive(OpenPgpElement element)
            throws XmlPullParserException, MissingOpenPgpKeyPairException, SmackOpenPgpException, IOException {
        byte[] decoded = Base64.decode(element.getEncryptedBase64MessageContent());

        DecryptedBytesAndMetadata decryptedBytes = cryptoProvider.decrypt(decoded, getJid(), null);

        OpenPgpMessage openPgpMessage = new OpenPgpMessage(decryptedBytes.getBytes(),
                new OpenPgpMessage.Metadata(decryptedBytes.getDecryptionKey(),
                        decryptedBytes.getVerifiedSignatures()));

        return openPgpMessage.getOpenPgpContentElement();
    }
}
