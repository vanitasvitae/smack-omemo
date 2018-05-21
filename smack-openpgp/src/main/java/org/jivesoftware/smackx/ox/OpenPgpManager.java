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

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.pep.PEPListener;
import org.jivesoftware.smackx.pep.PEPManager;
import org.jivesoftware.smackx.pubsub.EventElement;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

public final class OpenPgpManager extends Manager implements PEPListener {

    public static final String PEP_NODE_PUBLIC_KEYS = "urn:xmpp:openpgp:0:public-keys";
    public static final String PEP_NODE_PUBLIC_KEYS_NOTIFY = PEP_NODE_PUBLIC_KEYS + "+notify";

    public static String PEP_NODE_PUBLIC_KEY(String id) {
        return PEP_NODE_PUBLIC_KEYS + ":" + id;
    }

    private static final Map<XMPPConnection, OpenPgpManager> INSTANCES = new WeakHashMap<>();
    private OpenPgpProvider provider;

    private OpenPgpManager(XMPPConnection connection) {
        super(connection);

        // Subscribe to public key changes
        PEPManager.getInstanceFor(connection()).addPEPListener(this);
        ServiceDiscoveryManager.getInstanceFor(connection())
                .addFeature(PEP_NODE_PUBLIC_KEYS_NOTIFY);
    }

    public static OpenPgpManager getInstanceFor(XMPPConnection connection) {
        OpenPgpManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new OpenPgpManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    public void setOpenPgpProvider(OpenPgpProvider provider) {
        this.provider = provider;

    }

    public void publishPublicKey() throws Exception {
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
            keyNode.publish(new PayloadItem<>(pubkeyElement));
        }

        // Publish ID to metadata node
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

        metadataNode.publish(new PayloadItem<>(builder.build()));
    }

    public PublicKeysListElement fetchPubkeysList()
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        return fetchPubkeysList(connection().getUser().asBareJid());
    }

    public PublicKeysListElement fetchPubkeysList(BareJid jid)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException
    {
        PubSubManager pm = PubSubManager.getInstance(connection(), jid);

        LeafNode node = pm.getLeafNode(PEP_NODE_PUBLIC_KEYS);
        List<PayloadItem<PublicKeysListElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    public PubkeyElement fetchPubkey(BareJid jid, String v4_fingerprint)
            throws InterruptedException, PubSubException.NotALeafNodeException, SmackException.NoResponseException,
            SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            PubSubException.NotAPubSubNodeException {
        PubSubManager pm = PubSubManager.getInstance(connection(), jid);

        LeafNode node = pm.getLeafNode(PEP_NODE_PUBLIC_KEY(v4_fingerprint));
        List<PayloadItem<PubkeyElement>> list = node.getItems(1);

        if (list.isEmpty()) {
            return null;
        }

        return list.get(0).getPayload();
    }

    public void depositSecretKey() {
        ensureProviderIsSet();
        // Create key backup by appending serialized unencrypted secret keys.
        // Encrypt the backup using a random generated password
        // Publish the backup to the secret key node (whitelist protected)
        // Display the backup key to the user
    }

    public String getOurFingerprint() throws Exception {
        ensureProviderIsSet();
        return provider.getFingerprint();
    }

    /**
     * Throw an {@link IllegalStateException} if no {@link OpenPgpProvider} is set.
     */
    private void ensureProviderIsSet() {
        if (provider == null) {
            throw new IllegalStateException("No OpenPgpProvider set!");
        }
    }

    @Override
    public void eventReceived(EntityBareJid from, EventElement event, Message message) {

    }
}
