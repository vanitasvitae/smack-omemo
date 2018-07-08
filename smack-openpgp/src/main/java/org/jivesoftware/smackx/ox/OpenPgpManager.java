/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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

import static org.jivesoftware.smackx.ox.util.PubSubDelegate.PEP_NODE_PUBLIC_KEYS;
import static org.jivesoftware.smackx.ox.util.PubSubDelegate.PEP_NODE_PUBLIC_KEYS_NOTIFY;
import static org.jivesoftware.smackx.ox.util.PubSubDelegate.publishPublicKey;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.callback.backup.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyBackupSelectionCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.crypto.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.NoBackupFoundException;
import org.jivesoftware.smackx.ox.listener.internal.CryptElementReceivedListener;
import org.jivesoftware.smackx.ox.listener.internal.SignElementReceivedListener;
import org.jivesoftware.smackx.ox.listener.internal.SigncryptElementReceivedListener;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.util.PubSubDelegate;
import org.jivesoftware.smackx.ox.util.SecretKeyBackupHelper;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubFeature;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.pgpainless.pgpainless.PGPainless;
import org.pgpainless.pgpainless.key.OpenPgpV4Fingerprint;
import org.xmlpull.v1.XmlPullParserException;

public final class OpenPgpManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpManager.class.getName());

    /**
     * Map of instances.
     */
    private static final Map<XMPPConnection, OpenPgpManager> INSTANCES = new WeakHashMap<>();

    /**
     * {@link OpenPgpProvider} responsible for processing keys, encrypting and decrypting messages and so on.
     */
    private OpenPgpProvider provider;

    private final Map<BareJid, OpenPgpContact> openPgpCapableContacts = new HashMap<>();

    private final Set<SigncryptElementReceivedListener> signcryptElementReceivedListeners = new HashSet<>();
    private final Set<SignElementReceivedListener> signElementReceivedListeners = new HashSet<>();
    private final Set<CryptElementReceivedListener> cryptElementReceivedListeners = new HashSet<>();

    private OpenPgpSelf self;

    /**
     * Private constructor to avoid instantiation without putting the object into {@code INSTANCES}.
     *
     * @param connection xmpp connection.
     */
    private OpenPgpManager(XMPPConnection connection) {
        super(connection);
        ChatManager.getInstanceFor(connection).addIncomingListener(incomingOpenPgpMessageListener);
    }

    /**
     * Get the instance of the {@link OpenPgpManager} which belongs to the {@code connection}.
     *
     * @param connection xmpp connection.
     * @return instance of the manager.
     */
    public static OpenPgpManager getInstanceFor(XMPPConnection connection) {
        OpenPgpManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OpenPgpManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public BareJid getJidOrThrow() throws SmackException.NotLoggedInException {
        throwIfNotAuthenticated();
        return connection().getUser().asEntityBareJidOrThrow();
    }

    /**
     * Set the {@link OpenPgpProvider} which will be used to process incoming OpenPGP elements,
     * as well as to execute cryptographic operations.
     *
     * @param provider OpenPgpProvider.
     */
    public void setOpenPgpProvider(OpenPgpProvider provider) {
        this.provider = provider;
    }

    OpenPgpProvider getProvider() {
        return provider;
    }

    /**
     * Get our OpenPGP self.
     *
     * @return self
     * @throws SmackException.NotLoggedInException if we are not logged in
     */
    public OpenPgpSelf getOpenPgpSelf() throws SmackException.NotLoggedInException {
        throwIfNoProviderSet();

        if (self == null) {
            self = new OpenPgpSelf(getJidOrThrow(), provider.getStore());
        }

        return self;
    }

    /**
     * Generate a fresh OpenPGP key pair, given we don't have one already.
     * Publish the public key to the Public Key Node and update the Public Key Metadata Node with our keys fingerprint.
     * Lastly register a {@link PEPListener} which listens for updates to Public Key Metadata Nodes.
     *
     * @throws NoSuchAlgorithmException if we are missing an algorithm to generate a fresh key pair.
     * @throws NoSuchProviderException if we are missing a suitable {@link java.security.Provider}.
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if one of the PubSub nodes is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws IOException IO is dangerous.
     * @throws InvalidAlgorithmParameterException if illegal algorithm parameters are used for key generation.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     * @throws PGPException if something goes wrong during key loading/generating
     */
    public void announceSupportAndPublish()
            throws NoSuchAlgorithmException, NoSuchProviderException, InterruptedException,
            PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, IOException,
            InvalidAlgorithmParameterException, SmackException.NotLoggedInException, PGPException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();

        OpenPgpV4Fingerprint primaryFingerprint = getOurFingerprint();

        if (primaryFingerprint == null) {
            primaryFingerprint = generateAndImportKeyPair(getJidOrThrow());
        }

        // Create <pubkey/> element
        PubkeyElement pubkeyElement;
        try {
            pubkeyElement = createPubkeyElement(getJidOrThrow(), primaryFingerprint, new Date());
        } catch (MissingOpenPgpPublicKeyException e) {
            throw new AssertionError("Cannot publish our public key, since it is missing (MUST NOT happen!)");
        }

        // publish it
        publishPublicKey(connection(), pubkeyElement, primaryFingerprint);

        // Subscribe to public key changes
        // PEPManager.getInstanceFor(connection()).addPEPListener(metadataListener);
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(PEP_NODE_PUBLIC_KEYS_NOTIFY);
    }

    /**
     * Generate a fresh OpenPGP key pair and import it.
     *
     * @param ourJid our {@link BareJid}.
     * @return {@link OpenPgpV4Fingerprint} of the generated key.
     * @throws NoSuchAlgorithmException if the JVM doesn't support one of the used algorithms.
     * @throws InvalidAlgorithmParameterException if the used algorithm parameters are invalid.
     * @throws NoSuchProviderException if we are missing a cryptographic provider.
     */
    public OpenPgpV4Fingerprint generateAndImportKeyPair(BareJid ourJid)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException,
            PGPException, IOException {

        throwIfNoProviderSet();
        OpenPgpStore store = provider.getStore();
        PGPSecretKeyRing keyPair = store.generateKeyRing(ourJid);
        try {
            store.importSecretKey(ourJid, keyPair);
        } catch (MissingUserIdOnKeyException e) {
            // This should never throw, since we set our jid literally one line above this comment.
            throw new AssertionError(e);
        }

        return new OpenPgpV4Fingerprint(keyPair);
    }

    /**
     * Return the upper-case hex encoded OpenPGP v4 fingerprint of our key pair.
     *
     * @return fingerprint.
     */
    public OpenPgpV4Fingerprint getOurFingerprint()
            throws SmackException.NotLoggedInException, IOException, PGPException {
        return getOpenPgpSelf().getSigningKeyFingerprint();
    }

    /**
     * Return an OpenPGP capable contact.
     * This object can be used as an entry point to OpenPGP related API.
     *
     * @param jid {@link BareJid} of the contact.
     * @return {@link OpenPgpContact}.
     */
    public OpenPgpContact getOpenPgpContact(EntityBareJid jid) {
        throwIfNoProviderSet();
        return provider.getStore().getOpenPgpContact(jid);
    }


    /**
     * Determine, if we can sync secret keys using private PEP nodes as described in the XEP.
     * Requirements on the server side are support for PEP and support for the whitelist access model of PubSub.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @param connection XMPP connection
     * @return true, if the server supports secret key backups, otherwise false.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread is interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     */
    public static boolean serverSupportsSecretKeyBackups(XMPPConnection connection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection)
                .serverSupportsFeature(PubSubFeature.access_whitelist.toString());
    }

    /**
     * Upload the encrypted secret key to a private PEP node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @param displayCodeCallback callback, which will receive the backup password used to encrypt the secret key.
     * @param selectKeyCallback callback, which will receive the users choice of which keys will be backed up.
     * @throws InterruptedException if the thread is interrupted.
     * @throws PubSubException.NotALeafNodeException if the private node is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     * @throws IOException IO is dangerous.
     * @throws SmackException.FeatureNotSupportedException if the server doesn't support the PubSub whitelist access model.
     */
    public void backupSecretKeyToServer(DisplayBackupCodeCallback displayCodeCallback,
                                        SecretKeyBackupSelectionCallback selectKeyCallback)
            throws InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            SmackException.NotLoggedInException, IOException,
            SmackException.FeatureNotSupportedException, PGPException, MissingOpenPgpKeyPairException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();

        BareJid ownJid = connection().getUser().asBareJid();

        String backupCode = SecretKeyBackupHelper.generateBackupPassword();

        PGPSecretKeyRingCollection secretKeyRings = provider.getStore().getSecretKeysOf(ownJid);

        Set<OpenPgpV4Fingerprint> availableKeyPairs = new HashSet<>();
        for (PGPSecretKeyRing ring : secretKeyRings) {
            availableKeyPairs.add(new OpenPgpV4Fingerprint(ring));
        }

        Set<OpenPgpV4Fingerprint> selectedKeyPairs = selectKeyCallback.selectKeysToBackup(availableKeyPairs);

        SecretkeyElement secretKey = SecretKeyBackupHelper.createSecretkeyElement(provider, ownJid, selectedKeyPairs, backupCode);

        PubSubDelegate.depositSecretKey(connection(), secretKey);
        displayCodeCallback.displayBackupCode(backupCode);
    }

    /**
     * Delete the private {@link LeafNode} containing our secret key backup.
     *
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws InterruptedException if the thread gets interrupted.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws SmackException.NotLoggedInException if we are not logged in.
     */
    public void deleteSecretKeyServerBackup()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, SmackException.NotLoggedInException {
        throwIfNotAuthenticated();
        PubSubDelegate.deleteSecretKeyNode(connection());
    }

    /**
     * Fetch a secret key backup from the server and try to restore a selected secret key from it.
     *
     * @param codeCallback callback for prompting the user to provide the secret backup code.
     * @param selectionCallback callback allowing the user to select a secret key which will be restored.
     *
     * @return fingerprint of the restored secret key
     *
     * @throws InterruptedException if the thread gets interrupted.
     * @throws PubSubException.NotALeafNodeException if the private node is not a {@link LeafNode}.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws SmackException.NotConnectedException if we are not connected.
     * @throws SmackException.NoResponseException if the server doesn't respond.
     * @throws InvalidBackupCodeException if the user-provided backup code is invalid.
     * @throws SmackException.NotLoggedInException if we are not logged in
     * @throws IOException IO is dangerous
     * @throws MissingUserIdOnKeyException if the key that is to be imported is missing a user-id with our jid
     * @throws NoBackupFoundException if no secret key backup has been found
     */
    public OpenPgpV4Fingerprint restoreSecretKeyServerBackup(AskForBackupCodeCallback codeCallback,
                                                             SecretKeyRestoreSelectionCallback selectionCallback)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException,
            InvalidBackupCodeException, SmackException.NotLoggedInException, IOException, MissingUserIdOnKeyException,
            NoBackupFoundException, PGPException {
        throwIfNoProviderSet();
        throwIfNotAuthenticated();
        SecretkeyElement backup = PubSubDelegate.fetchSecretKey(connection());
        if (backup == null) {
            throw new NoBackupFoundException();
        }

        String backupCode = codeCallback.askForBackupCode();

        PGPSecretKeyRing key = SecretKeyBackupHelper.restoreSecretKeyBackup(backup, backupCode);
        provider.getStore().importSecretKey(getJidOrThrow(), key);
        return new OpenPgpV4Fingerprint(key);
    }

    /**
     * {@link PEPListener} that listens for changes to the OX public keys metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>
     */
    private final PEPListener metadataListener = new PEPListener() {
        @Override
        public void eventReceived(final EntityBareJid from, final EventElement event, final Message message) {
            if (PEP_NODE_PUBLIC_KEYS.equals(event.getEvent().getNode())) {
                final BareJid contact = from.asBareJid();
                LOGGER.log(Level.INFO, "Received OpenPGP metadata update from " + contact);
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        ItemsExtension items = (ItemsExtension) event.getExtensions().get(0);
                        PayloadItem<?> payload = (PayloadItem) items.getItems().get(0);
                        PublicKeysListElement listElement = (PublicKeysListElement) payload.getPayload();

                        processPublicKeysListElement(from, listElement);
                    }
                }, "ProcessOXMetadata");
            }
        }
    };

    private void processPublicKeysListElement(BareJid contact, PublicKeysListElement listElement) {

        try {
            PGPPublicKeyRingCollection contactsKeys = provider.getStore().getPublicKeysOf(contact);
            for (OpenPgpV4Fingerprint fingerprint : listElement.getMetadata().keySet()) {
                PGPPublicKeyRing key = contactsKeys.getPublicKeyRing(fingerprint.getKeyId());
                if (key == null) {

                }
            }
        } catch (PGPException | IOException e) {
            LOGGER.log(Level.WARNING, "Could not read public keys of " + contact, e);
        }
    }

    private final IncomingChatMessageListener incomingOpenPgpMessageListener =
            new IncomingChatMessageListener() {
                @Override
                public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
                    OpenPgpElement element = message.getExtension(OpenPgpElement.ELEMENT, OpenPgpElement.NAMESPACE);
                    if (element == null) {
                        // Message does not contain an OpenPgpElement -> discard
                        return;
                    }

                    OpenPgpContact contact = getOpenPgpContact(from);

                    OpenPgpContentElement contentElement = null;
                    try {
                        contentElement = provider.decryptAndOrVerify(element, self, contact).getOpenPgpContentElement();
                    } catch (PGPException e) {
                        LOGGER.log(Level.WARNING, "Could not decrypt incoming OpenPGP encrypted message", e);
                    } catch (XmlPullParserException | IOException e) {
                        LOGGER.log(Level.WARNING, "Invalid XML content of incoming OpenPGP encrypted message", e);
                    }

                    if (contentElement instanceof SigncryptElement) {
                        for (SigncryptElementReceivedListener l : signcryptElementReceivedListeners) {
                            l.signcryptElementReceived(contact, message, (SigncryptElement) contentElement);
                        }
                        return;
                    }

                    if (contentElement instanceof SignElement) {
                        for (SignElementReceivedListener l : signElementReceivedListeners) {
                            l.signElementReceived(contact, message, (SignElement) contentElement);
                        }
                        return;
                    }

                    if (contentElement instanceof CryptElement) {
                        for (CryptElementReceivedListener l : cryptElementReceivedListeners) {
                            l.cryptElementReceived(contact, message, (CryptElement) contentElement);
                        }
                        return;
                    }

                    else {
                        throw new AssertionError("Invalid element received: " + contentElement.getClass().getName());
                    }
                }
            };

    /*
    Private stuff.
     */

    /**
     * Process a {@link PubkeyElement}. This includes unpacking the key from the element and importing it.
     *
     * @param pubkeyElement {@link PubkeyElement} containing the key
     * @param owner owner of the key
     * @throws MissingUserIdOnKeyException if the key does not have an OpenPGP user-id of the form
     * "xmpp:juliet@capulet.lit" with the owners {@link BareJid}
     * @throws IOException row, row, row your byte gently down the {@link InputStream}
     */
    private void processPublicKey(PubkeyElement pubkeyElement, BareJid owner)
            throws MissingUserIdOnKeyException, IOException, PGPException {
        byte[] base64 = pubkeyElement.getDataElement().getB64Data();
        PGPPublicKeyRing keyRing = PGPainless.readKeyRing().publicKeyRing(Base64.decode(base64));
        provider.getStore().importPublicKey(owner, keyRing);
    }

    /**
     * Create a {@link PubkeyElement} which contains the OpenPGP public key of {@code owner} which belongs to
     * the {@link OpenPgpV4Fingerprint} {@code fingerprint}.
     *
     * @param owner owner of the public key
     * @param fingerprint fingerprint of the key
     * @param date date of creation of the element
     * @return {@link PubkeyElement} containing the key
     *
     * @throws MissingOpenPgpPublicKeyException if the public key notated by the fingerprint cannot be found
     */
    private PubkeyElement createPubkeyElement(BareJid owner,
                                              OpenPgpV4Fingerprint fingerprint,
                                              Date date)
            throws MissingOpenPgpPublicKeyException, IOException, PGPException {
        PGPPublicKeyRingCollection publicKeyRingCollection = provider.getStore().getPublicKeysOf(owner);
        if (publicKeyRingCollection != null) {
            PGPPublicKeyRing keys = publicKeyRingCollection.getPublicKeyRing(fingerprint.getKeyId());
            if (keys != null) {
                byte[] keyBytes = keys.getEncoded(true);
                return createPubkeyElement(keyBytes, date);
            }
        }
        throw new MissingOpenPgpPublicKeyException(owner, fingerprint);
    }

    /**
     * Create a {@link PubkeyElement} which contains the given {@code data} base64 encoded.
     *
     * @param bytes byte representation of an OpenPGP public key
     * @param date date of creation of the element
     * @return {@link PubkeyElement} containing the key
     */
    private static PubkeyElement createPubkeyElement(byte[] bytes, Date date) {
        return new PubkeyElement(new PubkeyElement.PubkeyDataElement(Base64.encode(bytes)), date);
    }

    /**
     * Register a {@link SigncryptElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link SigncryptElement} has been received and successfully decrypted.
     *
     * Note: This method is not intended for clients to listen for incoming {@link SigncryptElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    void registerSigncryptReceivedListener(SigncryptElementReceivedListener listener) {
        signcryptElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link SigncryptElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link SigncryptElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterSigncryptElementReceivedListener(SigncryptElementReceivedListener listener) {
        signcryptElementReceivedListeners.remove(listener);
    }

    /**
     * Register a {@link SignElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link SignElement} has been received and successfully verified.
     *
     * Note: This method is not intended for clients to listen for incoming {@link SignElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    void registerSignElementReceivedListener(SignElementReceivedListener listener) {
        signElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link SignElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link SignElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterSignElementReceivedListener(SignElementReceivedListener listener) {
        signElementReceivedListeners.remove(listener);
    }

    /**
     * Register a {@link CryptElementReceivedListener} on the {@link OpenPgpManager}.
     * That listener will get informed whenever a {@link CryptElement} has been received and successfully decrypted.
     *
     * Note: This method is not intended for clients to listen for incoming {@link CryptElement}s.
     * Instead its purpose is to allow easy extension of XEP-0373 for custom OpenPGP profiles such as
     * OpenPGP for XMPP: Instant Messaging.
     *
     * @param listener listener that gets registered
     */
    void registerCryptElementReceivedListener(CryptElementReceivedListener listener) {
        cryptElementReceivedListeners.add(listener);
    }

    /**
     * Unregister a prior registered {@link CryptElementReceivedListener}. That listener will no longer get
     * informed about incoming decrypted {@link CryptElement}s.
     *
     * @param listener listener that gets unregistered
     */
    void unregisterCryptElementReceivedListener(CryptElementReceivedListener listener) {
        cryptElementReceivedListeners.remove(listener);
    }

    /**
     * Throw an {@link IllegalStateException} if no {@link OpenPgpProvider} is set.
     * The OpenPgpProvider is used to process information related to RFC-4880.
     */
    private void throwIfNoProviderSet() {
        if (provider == null) {
            throw new IllegalStateException("No OpenPgpProvider set!");
        }
    }

    /**
     * Throw a {@link org.jivesoftware.smack.SmackException.NotLoggedInException} if the {@link XMPPConnection} of this
     * manager is not authenticated at this point.
     *
     * @throws SmackException.NotLoggedInException if we are not authenticated
     */
    private void throwIfNotAuthenticated() throws SmackException.NotLoggedInException {
        if (!connection().isAuthenticated()) {
            throw new SmackException.NotLoggedInException();
        }
    }
}
