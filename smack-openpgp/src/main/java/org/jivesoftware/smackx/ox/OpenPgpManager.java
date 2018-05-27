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

import static org.jivesoftware.smackx.ox.PubSubDelegate.PEP_NODE_PUBLIC_KEYS;
import static org.jivesoftware.smackx.ox.PubSubDelegate.PEP_NODE_PUBLIC_KEYS_NOTIFY;

import java.security.SecureRandom;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.callback.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.SecretKeyRestoreSelectionCallback;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.jxmpp.jid.EntityBareJid;

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

    /**
     * Private constructor to avoid instantiation without putting the object into {@code INSTANCES}.
     *
     * @param connection xmpp connection.
     */
    private OpenPgpManager(XMPPConnection connection) {
        super(connection);

        // Subscribe to public key changes
        PEPManager.getInstanceFor(connection()).addPEPListener(metadataListener);
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(PEP_NODE_PUBLIC_KEYS_NOTIFY);
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

    /**
     * Set the {@link OpenPgpProvider} which will be used to process incoming OpenPGP elements,
     * as well as to execute cryptographic operations.
     *
     * @param provider OpenPgpProvider.
     */
    public void setOpenPgpProvider(OpenPgpProvider provider) {
        this.provider = provider;
    }

    /**
     * Return the upper-case hex encoded OpenPGP v4 fingerprint of our key pair.
     *
     * @return fingerprint.
     * @throws CorruptedOpenPgpKeyException if for some reason we cannot determine our fingerprint.
     */
    public OpenPgpV4Fingerprint getOurFingerprint() throws CorruptedOpenPgpKeyException {
        throwIfNoProviderSet();
        return provider.primaryOpenPgpKeyPairFingerprint();
    }

    /**
     * Determine, if we can sync secret keys using private PEP nodes as described in the XEP.
     * Requirements on the server side are support for PEP and support for the whitelist access model of PubSub.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @return true, if the server supports secret key backups, otherwise false.
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean serverSupportsSecretKeyBackups()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        boolean pep = PEPManager.getInstanceFor(connection()).isSupported();
        boolean whitelist = PubSubManager.getInstance(connection(), connection().getUser().asBareJid())
                .getSupportedFeatures().containsFeature("http://jabber.org/protocol/pubsub#access-whitelist");
        return pep && whitelist;
    }

    /**
     * Upload the encrypted secret key to a private PEP node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a>
     *
     * @param callback callback, which will receive the backup password used to encrypt the secret key.
     * @throws CorruptedOpenPgpKeyException if the secret key is corrupted or can for some reason not be serialized.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public void backupSecretKeyToServer(DisplayBackupCodeCallback callback)
            throws CorruptedOpenPgpKeyException, InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            MissingOpenPgpKeyPairException {
        throwIfNoProviderSet();
        String backupCode = generateBackupPassword();
        SecretkeyElement secretKey = provider.createSecretkeyElement(null, backupCode); // TODO
        PubSubDelegate.depositSecretKey(connection(), secretKey);
        callback.displayBackupCode(backupCode);
    }

    public void deleteSecretKeyServerBackup()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubDelegate.deleteSecretKeyNode(connection());
    }

    public void restoreSecretKeyServerBackup(AskForBackupCodeCallback codeCallback,
                                             SecretKeyRestoreSelectionCallback selectionCallback)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, CorruptedOpenPgpKeyException,
            InvalidBackupCodeException {
        throwIfNoProviderSet();
        SecretkeyElement backup = PubSubDelegate.fetchSecretKey(connection());
        provider.restoreSecretKeyBackup(backup, codeCallback.askForBackupCode(), selectionCallback);
        // TODO: catch InvalidBackupCodeException in order to prevent re-fetching the backup on next try.
    }

    /**
     * {@link PEPListener} that listens for changes to the OX public keys metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>
     */
    private final PEPListener metadataListener = new PEPListener() {
        @Override
        public void eventReceived(final EntityBareJid from, final EventElement event, Message message) {
            if (PEP_NODE_PUBLIC_KEYS.equals(event.getEvent().getNode())) {
                LOGGER.log(Level.INFO, "Received OpenPGP metadata update from " + from);
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        ItemsExtension items = (ItemsExtension) event.getExtensions().get(0);
                        PayloadItem<?> payload = (PayloadItem) items.getItems().get(0);
                        PublicKeysListElement listElement = (PublicKeysListElement) payload.getPayload();

                        try {
                            provider.storePublicKeysList(connection(), listElement, from.asBareJid());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error processing OpenPGP metadata update from " + from + ".", e);
                        }
                    }
                }, "ProcessOXMetadata");
            }
        }
    };

    /**
     * Generate a secure backup code.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#sect-idm140425111347232">XEP-0373 §5.3</a>
     * @return backup code
     */
    private static String generateBackupPassword() {
        final String alphabet = "123456789ABCDEFGHIJKLMNPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        // 6 blocks
        for (int i = 0; i < 6; i++) {

            // of 4 chars
            for (int j = 0; j < 4; j++) {
                char c = alphabet.charAt(random.nextInt(alphabet.length()));
                code.append(c);
            }

            // dash after every block except the last one
            if (i != 5) {
                code.append('-');
            }
        }
        return code.toString();
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
}
