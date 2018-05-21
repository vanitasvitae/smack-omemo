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

import java.util.Date;
import java.util.List;
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
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemsExtension;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

public final class OpenPgpManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(OpenPgpManager.class.getName());

    /**
     * Name of the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#announcing-pubkey-list">XEP-0373 §4.2</a>.
     */
    public static final String PEP_NODE_PUBLIC_KEYS = "urn:xmpp:openpgp:0:public-keys";

    /**
     * Feature to be announced using the {@link ServiceDiscoveryManager} to subscribe to the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>.
     */
    public static final String PEP_NODE_PUBLIC_KEYS_NOTIFY = PEP_NODE_PUBLIC_KEYS + "+notify";

    /**
     * Name of the OX public key node, which contains the key with id {@code id}.
     *
     * @param id upper case hex encoded OpenPGP v4 fingerprint of the key.
     * @return PEP node name.
     */
    public static String PEP_NODE_PUBLIC_KEY(String id) {
        return PEP_NODE_PUBLIC_KEYS + ":" + id;
    }

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
     * Publish the users OpenPGP public key to the public key node if necessary.
     * Also announce the key to other users by updating the metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#annoucning-pubkey">XEP-0373 §4.1</a>.
     *
     * @throws CorruptedOpenPgpKeyException if our OpenPGP key is corrupted and for that reason cannot be serialized.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public void publishPublicKey()
            throws CorruptedOpenPgpKeyException, InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        ensureProviderIsSet();
        PubkeyElement pubkeyElement = provider.createPubkeyElement();

        String fingerprint = provider.getFingerprint();
        String keyNodeName = PEP_NODE_PUBLIC_KEY(fingerprint);
        PubSubManager pm = PubSubManager.getInstance(connection(), connection().getUser().asBareJid());

        // Check if key available at data node
        // If not, publish key to data node
        LeafNode keyNode = pm.getOrCreateLeafNode(keyNodeName);
        List<Item> items = keyNode.getItems(1);
        if (items.isEmpty()) {
            LOGGER.log(Level.FINE, "Node " + keyNodeName + " is empty. Publish.");
            keyNode.publish(new PayloadItem<>(pubkeyElement));
        } else {
            LOGGER.log(Level.FINE, "Node " + keyNodeName + " already contains key. Skip.");
        }

        // Fetch IDs from metadata node
        LeafNode metadataNode = pm.getOrCreateLeafNode(PEP_NODE_PUBLIC_KEYS);
        List<PayloadItem<PublicKeysListElement>> metadataItems = metadataNode.getItems(1);

        PublicKeysListElement.Builder builder = PublicKeysListElement.builder();
        if (!metadataItems.isEmpty() && metadataItems.get(0).getPayload() != null) {
            // Add old entries back to list.
            PublicKeysListElement publishedList = metadataItems.get(0).getPayload();
            for (PublicKeysListElement.PubkeyMetadataElement meta : publishedList.getMetadata().values()) {
                builder.addMetadata(meta);
            }
        }
        builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fingerprint, new Date()));

        // Publish IDs to metadata node
        metadataNode.publish(new PayloadItem<>(builder.build()));
    }

    /**
     * Consult the public key metadata node and fetch a list of all of our published OpenPGP public keys.
     * TODO: Add @see which points to the (for now missing) respective example in XEP-0373.
     *
     * @return content of our metadata node.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotAPubSubNodeException
     */
    public PublicKeysListElement fetchPubkeysList()
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        return fetchPubkeysList(connection().getUser().asBareJid());
    }

    /**
     * Consult the public key metadata node of {@code contact} to fetch the list of their published OpenPGP public keys.
     * TODO: Add @see which points to the (for now missing) respective example in XEP-0373.
     *
     * @param contact {@link BareJid} of the user we want to fetch the list from.
     * @return content of {@code contact}'s metadata node.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotAPubSubNodeException
     */
    public PublicKeysListElement fetchPubkeysList(BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstance(connection(), contact);

        LeafNode node = pm.getLeafNode(PEP_NODE_PUBLIC_KEYS);
        List<PayloadItem<PublicKeysListElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    /**
     * Delete our metadata node.
     *
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public void deletePubkeysListNode()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstance(connection(), connection().getUser().asBareJid());
        pm.deleteNode(PEP_NODE_PUBLIC_KEYS);
    }

    /**
     * Fetch the OpenPGP public key of a {@code contact}, identified by its OpenPGP {@code v4_fingerprint}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#discover-pubkey">XEP-0373 §4.3</a>.
     *
     * @param contact {@link BareJid} of the contact we want to fetch a key from.
     * @param v4_fingerprint upper case, hex encoded v4 fingerprint of the contacts key.
     * @return {@link PubkeyElement} containing the requested public key.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotAPubSubNodeException
     */
    public PubkeyElement fetchPubkey(BareJid contact, String v4_fingerprint)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstance(connection(), contact);

        LeafNode node = pm.getLeafNode(PEP_NODE_PUBLIC_KEY(v4_fingerprint));
        List<PayloadItem<PubkeyElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    /**
     * TODO: Implement and document.
     */
    public void depositSecretKey() {
        ensureProviderIsSet();
        // Create key backup by appending serialized unencrypted secret keys.
        // Encrypt the backup using a random generated password
        // Publish the backup to the secret key node (whitelist protected)
        // Display the backup key to the user
    }

    /**
     * Return the upper-case hex encoded OpenPGP v4 fingerprint of our key pair.
     *
     * @return fingerprint.
     * @throws CorruptedOpenPgpKeyException if for some reason we cannot determine our fingerprint.
     */
    public String getOurFingerprint() throws CorruptedOpenPgpKeyException {
        ensureProviderIsSet();
        return provider.getFingerprint();
    }

    /**
     * Throw an {@link IllegalStateException} if no {@link OpenPgpProvider} is set.
     * The OpenPgpProvider is used to process information related to RFC-4880.
     */
    private void ensureProviderIsSet() {
        if (provider == null) {
            throw new IllegalStateException("No OpenPgpProvider set!");
        }
    }

    /**
     * Determine, if we can sync secret keys using private PEP nodes as described in the XEP.
     * Requirements on the server side are support for PEP and support for the whitelist access model of PubSub.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">XEP-0373 §5</a> for more information.
     *
     * @return
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean canSyncSecretKey()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        boolean pep = PEPManager.getInstanceFor(connection()).isSupported();
        boolean whitelist = PubSubManager.getInstance(connection(), connection().getUser().asBareJid())
                .getSupportedFeatures().containsFeature("http://jabber.org/protocol/pubsub#access-whitelist");
        return pep && whitelist;
    }

    /**
     * {@link PEPListener} that listens for changes to the OX public keys metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>.
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
                            provider.processPublicKeysListElement(listElement, from.asBareJid());
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error processing OpenPGP metadata update from " + from, e);
                        }
                    }
                }, "ProcessOXPublicKey");
            }
        }
    };
}
