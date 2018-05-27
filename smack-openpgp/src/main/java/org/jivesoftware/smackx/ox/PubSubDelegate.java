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

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.CorruptedOpenPgpKeyException;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.BareJid;

public class PubSubDelegate {

    private static final Logger LOGGER = Logger.getLogger(PubSubDelegate.class.getName());

    /**
     * Name of the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#announcing-pubkey-list">XEP-0373 §4.2</a>
     */
    public static final String PEP_NODE_PUBLIC_KEYS = "urn:xmpp:openpgp:0:public-keys";

    /**
     * Name of the OX secret key node.
     */
    public static final String PEP_NODE_SECRET_KEY = "urn:xmpp:openpgp:0:secret-key";

    /**
     * Feature to be announced using the {@link ServiceDiscoveryManager} to subscribe to the OX metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#pubsub-notifications">XEP-0373 §4.4</a>
     */
    public static final String PEP_NODE_PUBLIC_KEYS_NOTIFY = PEP_NODE_PUBLIC_KEYS + "+notify";

    /**
     * Name of the OX public key node, which contains the key with id {@code id}.
     *
     * @param id upper case hex encoded OpenPGP v4 fingerprint of the key.
     * @return PEP node name.
     */
    public static String PEP_NODE_PUBLIC_KEY(OpenPgpV4Fingerprint id) {
        return PEP_NODE_PUBLIC_KEYS + ":" + id;
    }


    public static void changeAccessModelIfNecessary(LeafNode node, AccessModel accessModel)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        ConfigureForm current = node.getNodeConfiguration();
        if (current.getAccessModel() != accessModel) {
            ConfigureForm updateConfig = new ConfigureForm(DataForm.Type.submit);
            updateConfig.setAccessModel(accessModel);
            node.sendConfigurationForm(updateConfig);
        }
    }

    /**
     * Publish the users OpenPGP public key to the public key node if necessary.
     * Also announce the key to other users by updating the metadata node.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#annoucning-pubkey">XEP-0373 §4.1</a>
     *
     * @throws CorruptedOpenPgpKeyException if our OpenPGP key is corrupted and for that reason cannot
     * be serialized.
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public static void publishPublicKey(XMPPConnection connection, PubkeyElement pubkeyElement, OpenPgpV4Fingerprint fingerprint)
            throws CorruptedOpenPgpKeyException, InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {

        String keyNodeName = PEP_NODE_PUBLIC_KEY(fingerprint);
        PubSubManager pm = PubSubManager.getInstance(connection, connection.getUser().asBareJid());

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
    public static PublicKeysListElement fetchPubkeysList(XMPPConnection connection)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        return fetchPubkeysList(connection, connection.getUser().asBareJid());
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
    public static PublicKeysListElement fetchPubkeysList(XMPPConnection connection, BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstance(connection, contact);

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
    public static void deletePubkeysListNode(XMPPConnection connection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstance(connection, connection.getUser().asBareJid());
        pm.deleteNode(PEP_NODE_PUBLIC_KEYS);
    }

    /**
     * Fetch the OpenPGP public key of a {@code contact}, identified by its OpenPGP {@code v4_fingerprint}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#discover-pubkey">XEP-0373 §4.3</a>
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
    public static PubkeyElement fetchPubkey(XMPPConnection connection, BareJid contact, OpenPgpV4Fingerprint v4_fingerprint)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstance(connection, contact);

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
    public static void depositSecretKey(XMPPConnection connection, SecretkeyElement element)
            throws InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {

        PubSubManager pm = PubSubManager.getInstance(connection);
        LeafNode secretKeyNode = pm.getOrCreateLeafNode(PEP_NODE_SECRET_KEY);
        PubSubDelegate.changeAccessModelIfNecessary(secretKeyNode, AccessModel.whitelist);

        secretKeyNode.publish(new PayloadItem<>(element));
    }

    public static SecretkeyElement fetchSecretKey(XMPPConnection connection)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstance(connection);
        LeafNode secretKeyNode = pm.getOrCreateLeafNode(PEP_NODE_SECRET_KEY);
        List<PayloadItem<SecretkeyElement>> list = secretKeyNode.getItems(1);
        if (list.size() == 0) {
            LOGGER.log(Level.INFO, "No secret key published!");
            return null;
        }
        SecretkeyElement secretkeyElement = list.get(0).getPayload();
        return secretkeyElement;
    }

    public static void deleteSecretKeyNode(XMPPConnection connection)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        PubSubManager pm = PubSubManager.getInstance(connection);
        pm.deleteNode(PEP_NODE_SECRET_KEY);
    }
}
