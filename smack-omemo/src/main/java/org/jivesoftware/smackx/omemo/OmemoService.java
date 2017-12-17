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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.PEP_NODE_DEVICE_LIST;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.carbons.CarbonCopyReceivedListener;
import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.element.OmemoBundleVAxolotlElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListVAxolotlElement;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoVAxolotlElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.ClearTextMessage;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;
import org.jivesoftware.smackx.omemo.internal.listener.OmemoCarbonCopyStanzaReceivedListener;
import org.jivesoftware.smackx.omemo.internal.listener.OmemoMessageStanzaReceivedListener;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

/**
 * This class contains OMEMO related logic and registers listeners etc.
 *
 * @param <T_IdKeyPair> IdentityKeyPair class
 * @param <T_IdKey>     IdentityKey class
 * @param <T_PreKey>    PreKey class
 * @param <T_SigPreKey> SignedPreKey class
 * @param <T_Sess>      Session class
 * @param <T_Addr>      Address class
 * @param <T_ECPub>     Elliptic Curve PublicKey class
 * @param <T_Bundle>    Bundle class
 * @param <T_Ciph>      Cipher class
 * @author Paul Schaub
 */
public abstract class OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        implements OmemoCarbonCopyStanzaReceivedListener, OmemoMessageStanzaReceivedListener
{
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    protected static final Logger LOGGER = Logger.getLogger(OmemoService.class.getName());

    /**
     * This is a singleton.
     */
    private static OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> INSTANCE;

    protected OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore;
    protected final HashMap<OmemoManager, OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>> omemoRatchets = new HashMap<>();

    /**
     * Return the singleton instance of this class. When no instance is set, throw an IllegalStateException instead.
     * @return instance.
     */
    public static OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("No OmemoService registered");
        }
        return INSTANCE;
    }

    /**
     * Set singleton instance. Throws an IllegalStateException, if there is already a service set as instance.
     *
     * @param omemoService instance
     */
    protected static void setInstance(OmemoService<?, ?, ?, ?, ?, ?, ?, ?, ?> omemoService) {
        if (INSTANCE != null) {
            throw new IllegalStateException("An OmemoService is already registered");
        }
        INSTANCE = omemoService;
    }

    /**
     * Returns true, if an instance of the service singleton is set. Otherwise return false.
     *
     * @return true, if instance is not null.
     */
    public static boolean isServiceRegistered() {
        return INSTANCE != null;
    }

    /**
     * Return the used omemoStore backend.
     * If there is no store backend set yet, set the default one (typically a file-based one).
     *
     * @return omemoStore backend
     */
    public OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoStoreBackend() {
        if (omemoStore == null) {
            omemoStore = createDefaultOmemoStoreBackend();
        }
        return omemoStore;
    }

    /**
     * Set an omemoStore as backend. Throws an IllegalStateException, if there is already a backend set.
     *
     * @param omemoStore store.
     */
    public void setOmemoStoreBackend(
            OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore)
    {
        if (this.omemoStore != null) {
            throw new IllegalStateException("An OmemoStore backend has already been set.");
        }
        this.omemoStore = omemoStore;
    }

    /**
     * Create a default OmemoStore object.
     *
     * @return default omemoStore.
     */
    public abstract OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createDefaultOmemoStoreBackend();

    /**
     * Create a new OmemoService object. This should only happen once.
     * When the service gets created, it tries a placeholder crypto function in order to test, if all necessary
     * algorithms are available on the system.
     *
     * @throws NoSuchPaddingException               When no Cipher could be instantiated.
     * @throws NoSuchAlgorithmException             when no Cipher could be instantiated.
     * @throws NoSuchProviderException              when BouncyCastle could not be found.
     * @throws InvalidAlgorithmParameterException   when the Cipher could not be initialized
     * @throws InvalidKeyException                  when the generated key is invalid
     * @throws UnsupportedEncodingException         when UTF8 is unavailable
     * @throws BadPaddingException                  when cipher.doFinal gets wrong padding
     * @throws IllegalBlockSizeException            when cipher.doFinal gets wrong Block size.
     */
    public OmemoService()
            throws NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException
    {
        // Check availability of algorithms and encodings needed for crypto
        checkAvailableAlgorithms();
    }

    void registerManager(OmemoManager manager) {
        omemoRatchets.put(manager, instantiateOmemoRatchet(manager, getOmemoStoreBackend()));
    }

    /**
     * Initialize OMEMO functionality for OmemoManager omemoManager.
     *
     * @param managerGuard OmemoManager we'd like to initialize.
     * @throws InterruptedException
     * @throws CorruptedOmemoKeyException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotLoggedInException
     * @throws PubSubException.NotALeafNodeException
     */
    void publish(OmemoManager.LoggedInOmemoManager managerGuard)
            throws InterruptedException, CorruptedOmemoKeyException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException,
            PubSubException.NotALeafNodeException
    {
        // Create new keys if necessary and publish to the server.
        publishBundle(managerGuard);

        // Get fresh device list from server
        refreshOwnDeviceList(managerGuard);
        publishDeviceIdIfNeeded(managerGuard, false);
    }

    /**
     * Test availability of required algorithms. We do this in advance, so we can simplify exception handling later.
     *
     * @throws NoSuchPaddingException
     * @throws UnsupportedEncodingException
     * @throws InvalidAlgorithmParameterException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     */
    protected static void checkAvailableAlgorithms()
            throws NoSuchPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException,
            InvalidKeyException
    {
        // Test crypto functions. The method below throws an exception, when crypto algorithms are missing.
        new OmemoMessageBuilder<>(null, null, "");
    }

    /**
     * Generate a new unique deviceId and regenerate new keys.
     *
     * @param managerGuard  OmemoManager we want to regenerate.
     * @throws CorruptedOmemoKeyException when freshly generated identityKey is invalid
     *                                  (should never ever happen *crosses fingers*)
     */
    void regenerate(OmemoManager.LoggedInOmemoManager managerGuard)
            throws CorruptedOmemoKeyException, InterruptedException, PubSubException.NotALeafNodeException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        int nDeviceId = OmemoManager.randomDeviceId();

        // Generate unique ID that is not already taken
        while (!getOmemoStoreBackend().isAvailableDeviceId(userDevice, nDeviceId)) {
            nDeviceId = OmemoManager.randomDeviceId();
        }

        getOmemoStoreBackend().purgeOwnDeviceKeys(userDevice);
        omemoManager.setDeviceId(nDeviceId);

        publish(managerGuard);
    }

    /**
     * Publish a bundle to the server.
     *
     * @param managerGuard OmemoManager
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws CorruptedOmemoKeyException
     * @throws XMPPException.XMPPErrorException
     */
    void publishBundle(OmemoManager.LoggedInOmemoManager managerGuard)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            CorruptedOmemoKeyException, XMPPException.XMPPErrorException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        Date lastSignedPreKeyRenewal = getOmemoStoreBackend().getDateOfLastSignedPreKeyRenewal(userDevice);
        if (OmemoConfiguration.getRenewOldSignedPreKeys() && lastSignedPreKeyRenewal != null) {
            if (System.currentTimeMillis() - lastSignedPreKeyRenewal.getTime()
                    > 1000L * 60 * 60 * OmemoConfiguration.getRenewOldSignedPreKeysAfterHours()) {
                LOGGER.log(Level.FINE, "Renewing signedPreKey");
                getOmemoStoreBackend().changeSignedPreKey(userDevice);
            }
        } else {
            getOmemoStoreBackend().setDateOfLastSignedPreKeyRenewal(userDevice, new Date());
        }

        // publish
        OmemoBundleElement bundleElement = getOmemoStoreBackend().packOmemoBundle(userDevice);

        PubSubManager.getInstance(omemoManager.getConnection(), userDevice.getJid())
                .tryToPublishAndPossibleAutoCreate(
                        OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID(userDevice.getDeviceId()),
                        new PayloadItem<>(bundleElement));
    }

    /**
     * Publish our deviceId in case it is not on the list already.
     * This method calls publishDeviceIdIfNeeded(omemoManager, deleteOtherDevices, false).
     * @param managerGuard          OmemoManager
     * @param deleteOtherDevices    Do we want to remove other devices from the list?
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    void publishDeviceIdIfNeeded(OmemoManager.LoggedInOmemoManager managerGuard, boolean deleteOtherDevices)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException
    {
        publishDeviceIdIfNeeded(managerGuard, deleteOtherDevices, false);
    }

    /**
     * Publish our deviceId in case it is not on the list already.
     *
     * @param managerGuard       OmemoManager
     * @param deleteOtherDevices Do we want to remove other devices from the list?
     *                           If we do, publish the list with only our id, regardless if we were on the list
     *                           already.
     * @param publish            Do we want to force publishing our id?
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws XMPPException.XMPPErrorException
     * @throws PubSubException.NotALeafNodeException
     */
    void publishDeviceIdIfNeeded(OmemoManager.LoggedInOmemoManager managerGuard, boolean deleteOtherDevices, boolean publish)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        CachedDeviceList deviceList = getOmemoStoreBackend().loadCachedDeviceList(userDevice, userDevice.getJid());

        Set<Integer> deviceListIds;
        if (deviceList == null) {
            deviceListIds = new HashSet<>();
        } else {
            deviceListIds = new HashSet<>(deviceList.getActiveDevices());
        }

        if (deleteOtherDevices) {
            deviceListIds.clear();
        }

        int ourDeviceId = omemoManager.getDeviceId();
        if (deviceListIds.add(ourDeviceId)) {
            publish = true;
        }

        publish |= removeStaleDevicesIfNeeded(managerGuard, deviceListIds);

        if (publish) {
            publishDeviceIds(managerGuard, new OmemoDeviceListVAxolotlElement(deviceListIds));
        }
    }

    /**
     * Remove stale devices from our device list.
     * This does only delete devices, if that's configured in OmemoConfiguration.
     *
     * @param managerGuard  OmemoManager
     * @param deviceListIds deviceIds we plan to publish. Stale devices are deleted from that list.
     * @return
     */
    boolean removeStaleDevicesIfNeeded(OmemoManager.LoggedInOmemoManager managerGuard, Set<Integer> deviceListIds) {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();
        boolean publish = false;

        // Clear devices that we didn't receive a message from for a while
        Iterator<Integer> it = deviceListIds.iterator();
        while (OmemoConfiguration.getDeleteStaleDevices() && it.hasNext()) {

            int id = it.next();
            if (id == userDevice.getDeviceId()) {
                // Skip own id
                continue;
            }

            OmemoDevice contactsDevice = new OmemoDevice(userDevice.getJid(), id);
            Date date = getOmemoStoreBackend().getDateOfLastReceivedMessage(userDevice, contactsDevice);

            if (date == null) {

                getOmemoStoreBackend().setDateOfLastReceivedMessage(userDevice, contactsDevice, new Date());

            } else {

                if (System.currentTimeMillis() - date.getTime() > 1000L * 60 * 60 *
                        OmemoConfiguration.getDeleteStaleDevicesAfterHours()) {
                    LOGGER.log(Level.INFO, "Remove device " + id + " because of more than " +
                            OmemoConfiguration.getDeleteStaleDevicesAfterHours() + " hours of inactivity.");
                    it.remove();
                    publish = true;
                }
            }
        }
        return publish;
    }

    /**
     * Publish the given deviceList to the server.
     *omemoManager.getOwnJid()
     * @param managerGuard OmemoManager
     * @param deviceList list of deviceIDs
     * @throws InterruptedException                 Exception
     * @throws XMPPException.XMPPErrorException     Exception
     * @throws SmackException.NotConnectedException Exception
     * @throws SmackException.NoResponseException   Exception
     * @throws PubSubException.NotALeafNodeException Exception
     */
    static void publishDeviceIds(OmemoManager.LoggedInOmemoManager managerGuard, OmemoDeviceListElement deviceList)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException, PubSubException.NotALeafNodeException
    {
        OmemoManager omemoManager = managerGuard.get();
        PubSubManager.getInstance(omemoManager.getConnection(), omemoManager.getOwnJid())
                .tryToPublishAndPossibleAutoCreate(OmemoConstants.PEP_NODE_DEVICE_LIST, new PayloadItem<>(deviceList));
    }

    /**
     * Fetch the deviceList node of a contact.
     *
     * @param managerGuard omemoManager
     * @param contact contact
     * @return LeafNode
     * @throws InterruptedException
     * @throws PubSubException.NotALeafNodeException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     * @throws NotAPubSubNodeException
     */
    static LeafNode fetchDeviceListNode(OmemoManager.LoggedInOmemoManager managerGuard, BareJid contact)
            throws InterruptedException, PubSubException.NotALeafNodeException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException, NotAPubSubNodeException
    {
        OmemoManager omemoManager = managerGuard.get();
        return PubSubManager.getInstance(omemoManager.getConnection(), contact).getLeafNode(PEP_NODE_DEVICE_LIST);
    }

    /**
     * Directly fetch the device list of a contact.
     *
     * @param managerGuard OmemoManager
     * @param contact BareJid of the contact
     * @return The OmemoDeviceListElement of the contact
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     * @throws PubSubException.NotALeafNodeException when the device lists node is not a LeafNode
     * @throws NotAPubSubNodeException
     */
    static OmemoDeviceListElement fetchDeviceList(OmemoManager.LoggedInOmemoManager managerGuard, BareJid contact)
                    throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
                    SmackException.NoResponseException, PubSubException.NotALeafNodeException, NotAPubSubNodeException
    {
        return extractDeviceListFrom(fetchDeviceListNode(managerGuard, contact));
    }

    /**
     * Refresh our deviceList from the server.
     *
     * @param managerGuard omemoManager
     * @return true, if we should publish our device list again (because its broken or not existent...)
     *
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    private boolean refreshOwnDeviceList(OmemoManager.LoggedInOmemoManager managerGuard)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            XMPPException.XMPPErrorException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        try {
            getOmemoStoreBackend().mergeCachedDeviceList(userDevice, userDevice.getJid(),
                    fetchDeviceList(managerGuard, omemoManager.getOwnJid()));

        } catch (XMPPException.XMPPErrorException e) {

            if (e.getXMPPError().getCondition() == StanzaError.Condition.item_not_found) {
                LOGGER.log(Level.WARNING, "Could not refresh own deviceList, because the node did not exist: "
                        + e.getMessage());
                return true;
            }

            throw e;

        } catch (PubSubException.NotALeafNodeException e) {
            LOGGER.log(Level.WARNING, "Could not refresh own deviceList, because the Node is not a LeafNode: " +
                    e.getMessage());
            return true;
        }

        catch (PubSubException.NotAPubSubNodeException e) {
            LOGGER.log(Level.WARNING, "Caught a PubSubAssertionError when fetching a deviceList node. " +
                    "This probably means that we're dealing with an ejabberd server and the LeafNode does not exist.");
            return true;
        }
        return false;
    }

    /**
     * Refresh the deviceList of contact and merge it with the one stored locally.
     * @param managerGuard omemoManager
     * @param contact contact
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    void refreshDeviceList(OmemoManager.LoggedInOmemoManager managerGuard, BareJid contact)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException
    {
        OmemoDeviceListElement omemoDeviceListElement;

        try {
            omemoDeviceListElement = fetchDeviceList(managerGuard, contact);

        } catch (PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException | NotAPubSubNodeException e) {
            LOGGER.log(Level.WARNING, "Could not fetch device list of " + contact + ": " + e);
            return;
        }

        getOmemoStoreBackend().mergeCachedDeviceList(managerGuard.get().getOwnDevice(), contact, omemoDeviceListElement);
    }

    /**
     * Fetch the OmemoBundleElement of the contact.
     *
     * @param managerGuard OmemoManager
     * @param contact the contacts BareJid
     * @return the OmemoBundleElement of the contact
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     * @throws PubSubException.NotALeafNodeException when the bundles node is not a LeafNode
     * @throws NotAPubSubNodeException
     */
    static OmemoBundleVAxolotlElement fetchBundle(OmemoManager.LoggedInOmemoManager managerGuard, OmemoDevice contact)
                    throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
                    SmackException.NoResponseException, PubSubException.NotALeafNodeException, NotAPubSubNodeException
    {
        OmemoManager omemoManager = managerGuard.get();
        LeafNode node = PubSubManager.getInstance(omemoManager.getConnection(), contact.getJid()).getLeafNode(
                        PEP_NODE_BUNDLE_FROM_DEVICE_ID(contact.getDeviceId()));
        return extractBundleFrom(node);
    }

    /**
     * Extract the OmemoBundleElement of a contact from a LeafNode.
     *
     * @param node typically a LeafNode containing the OmemoBundles of a contact
     * @return the OmemoBundleElement
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    private static OmemoBundleVAxolotlElement extractBundleFrom(LeafNode node)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException
    {
        if (node == null) {
            return null;
        }
        try {
            return (OmemoBundleVAxolotlElement) ((PayloadItem<?>) node.getItems().get(0)).getPayload();
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Extract the OmemoDeviceListElement of a contact from a node containing his OmemoDeviceListElement.
     *
     * @param node typically a LeafNode containing the OmemoDeviceListElement of a contact
     * @return the extracted OmemoDeviceListElement.
     * @throws XMPPException.XMPPErrorException     When
     * @throws SmackException.NotConnectedException something
     * @throws InterruptedException                 goes
     * @throws SmackException.NoResponseException   wrong
     */
    private static OmemoDeviceListElement extractDeviceListFrom(LeafNode node)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException
    {
        if (node == null) {
            LOGGER.log(Level.WARNING, "DeviceListNode is null.");
            return null;
        }

        List<?> items = node.getItems();
        if (items.size() > 0) {

            OmemoDeviceListVAxolotlElement listElement =
                    (OmemoDeviceListVAxolotlElement) ((PayloadItem<?>) items.get(items.size() - 1)).getPayload();

            if (items.size() > 1) {
                node.deleteAllItems();
                node.publish(new PayloadItem<>(listElement));
            }

            return listElement;
        }

        Set<Integer> emptySet = Collections.emptySet();
        return new OmemoDeviceListVAxolotlElement(emptySet);
    }

    /**
     * Build sessions for all devices of the contact that we do not have a session with yet.
     *
     * @param managerGuard omemoManager
     * @param jid the BareJid of the contact
     */
    void buildMissingOmemoSessions(OmemoManager.LoggedInOmemoManager managerGuard, BareJid jid)
            throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            CannotEstablishOmemoSessionException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        CachedDeviceList devices = getOmemoStoreBackend().loadCachedDeviceList(userDevice, jid);
        CannotEstablishOmemoSessionException sessionException = null;

        if (devices == null || devices.getAllDevices().isEmpty()) {
            refreshDeviceList(managerGuard, jid);
            devices = getOmemoStoreBackend().loadCachedDeviceList(userDevice, jid);
        }

        for (int id : devices.getActiveDevices()) {

            OmemoDevice device = new OmemoDevice(jid, id);
            if (getOmemoStoreBackend().containsRawSession(userDevice, device)) {
                // We have a session already.
                continue;
            }

            // Build missing session
            try {
                buildSessionWithDevice(managerGuard, device, false);
            } catch (CannotEstablishOmemoSessionException e) {

                if (sessionException == null) {
                    sessionException = e;
                } else {
                    sessionException.addFailures(e);
                }

            } catch (CorruptedOmemoKeyException e) {
                CannotEstablishOmemoSessionException fail =
                        new CannotEstablishOmemoSessionException(device, e);

                if (sessionException == null) {
                    sessionException = fail;
                } else {
                    sessionException.addFailures(fail);
                }
            }
        }

        if (sessionException != null) {
            throw sessionException;
        }
    }

    /**
     * Build an OmemoSession for the given OmemoDevice.
     *
     * @param managerGuard omemoManager
     * @param contactsDevice OmemoDevice
     * @param fresh Do we want to build a session even if we already have one?
     * @throws CannotEstablishOmemoSessionException when no session could be established
     * @throws CorruptedOmemoKeyException when the bundle contained an invalid OMEMO identityKey
     */
    public void buildSessionWithDevice(OmemoManager.LoggedInOmemoManager managerGuard,
                                       OmemoDevice contactsDevice,
                                       boolean fresh)
            throws CannotEstablishOmemoSessionException, CorruptedOmemoKeyException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        if (contactsDevice.equals(omemoManager.getOwnDevice())) {
            return;
        }

        // Do not build sessions with devices we already know...
        if (!fresh && getOmemoStoreBackend().containsRawSession(userDevice, contactsDevice)) {
            return;
        }

        OmemoBundleVAxolotlElement bundle;
        try {
            bundle = fetchBundle(managerGuard, contactsDevice);
        } catch (SmackException | XMPPException.XMPPErrorException | InterruptedException e) {
            throw new CannotEstablishOmemoSessionException(contactsDevice, e);
        }

        HashMap<Integer, T_Bundle> bundles = getOmemoStoreBackend().keyUtil().BUNDLE.bundles(bundle, contactsDevice);

        // Select random Bundle
        int randomIndex = new Random().nextInt(bundles.size());
        T_Bundle randomPreKeyBundle = new ArrayList<>(bundles.values()).get(randomIndex);
        // Build raw session
        processBundle(managerGuard, randomPreKeyBundle, contactsDevice);
    }

    /**
     * Process a received bundle. Typically that includes saving keys and building a session.
     *
     * @param managerGuard omemoManager that will process the bundle
     * @param bundle T_Bundle (depends on used Signal/Olm library)
     * @param device OmemoDevice
     * @throws CorruptedOmemoKeyException
     */
    protected abstract void processBundle(OmemoManager.LoggedInOmemoManager managerGuard,
                                          T_Bundle bundle,
                                          OmemoDevice device)
            throws CorruptedOmemoKeyException;

    /**
     * Process a received message. Try to decrypt it in case we are a recipient device. If we are not a recipient
     * device, return null.
     *
     * @param contactsDevice    OmemoDevice of the sender of the message
     * @param message           the encrypted message
     * @param information       OmemoMessageInformation object which will contain meta data about the decrypted message
     * @return decrypted message or null
     * @throws NoRawSessionException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     * @throws SmackException.NotConnectedException
     * @throws CryptoFailedException
     * @throws XMPPException.XMPPErrorException
     * @throws CorruptedOmemoKeyException
     */
    private Message processReceivingMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                            OmemoDevice contactsDevice,
                                            OmemoElement message,
                                            final OmemoMessageInformation information)
            throws NoRawSessionException, InterruptedException, SmackException.NoResponseException,
            SmackException.NotConnectedException, CryptoFailedException, XMPPException.XMPPErrorException,
            CorruptedOmemoKeyException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        ArrayList<OmemoVAxolotlElement.OmemoHeader.Key> messageRecipientKeys = message.getHeader().getKeys();
        // Do we have a key with our ID in the message?
        for (OmemoVAxolotlElement.OmemoHeader.Key k : messageRecipientKeys) {
            // Only decrypt with our deviceID
            if (k.getId() != omemoManager.getDeviceId()) {
                continue;
            }

            Message decrypted = decryptOmemoMessageElement(managerGuard, contactsDevice, message, information);
            if (contactsDevice.equals(omemoManager.getOwnJid()) && decrypted != null) {
                getOmemoStoreBackend().setDateOfLastReceivedMessage(userDevice, contactsDevice, new Date());
            }
            return decrypted;
        }

        LOGGER.log(Level.INFO, "There is no key with our deviceId " + omemoManager.getDeviceId() + ". Silently discard the message.");
        return null;
    }

    /**
     * Decrypt a given OMEMO encrypted message. Return null, if there is no OMEMO element in the message,
     * otherwise try to decrypt the message and return a ClearTextMessage object.
     *
     * @param managerGuard omemoManager of the receiving device
     * @param sender barejid of the sender
     * @param message encrypted message
     * @return decrypted message or null
     * @throws InterruptedException                 Exception
     * @throws SmackException.NoResponseException   Exception
     * @throws SmackException.NotConnectedException Exception
     * @throws CryptoFailedException                When the message could not be decrypted.
     * @throws XMPPException.XMPPErrorException     Exception
     * @throws CorruptedOmemoKeyException           When the used OMEMO keys are invalid.
     * @throws NoRawSessionException                When there is no session to decrypt the message with in the double
     *                                              ratchet library
     */
    ClearTextMessage processLocalMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                         BareJid sender,
                                         Message message)
            throws InterruptedException, SmackException.NoResponseException, SmackException.NotConnectedException,
            CryptoFailedException, XMPPException.XMPPErrorException, CorruptedOmemoKeyException, NoRawSessionException
    {
        if (OmemoManager.stanzaContainsOmemoElement(message)) {
            OmemoElement omemoMessageElement = message.getExtension(OmemoElement.ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
            OmemoMessageInformation info = new OmemoMessageInformation();
            Message decrypted = processReceivingMessage(managerGuard,
                    new OmemoDevice(sender, omemoMessageElement.getHeader().getSid()),
                    omemoMessageElement, info);
            return new ClearTextMessage(decrypted != null ? decrypted.getBody() : null, message, info);
        } else {
            LOGGER.log(Level.WARNING, "Stanza does not contain an OMEMO message.");
            return null;
        }
    }

    /**
     * Encrypt a clear text message for the given recipient.
     * The body of the message will be encrypted.
     *
     * @param managerGuard omemoManager of the sending device
     * @param recipient BareJid of the recipient
     * @param message   message to encrypt.
     * @return OmemoMessageElement
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     * @throws NoSuchAlgorithmException
     */
    OmemoVAxolotlElement processSendingMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                               BareJid recipient,
                                               Message message)
            throws CryptoFailedException, UndecidedOmemoIdentityException, NoSuchAlgorithmException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            CannotEstablishOmemoSessionException
    {
        ArrayList<BareJid> recipients = new ArrayList<>();
        recipients.add(recipient);
        return processSendingMessage(managerGuard, recipients, message);
    }

    /**
     * Encrypt a clear text message for the given recipients.
     * The body of the message will be encrypted.
     *
     * @param managerGuard omemoManager of the sending device.
     * @param recipients List of BareJids of all recipients
     * @param message    message to encrypt.
     * @return OmemoMessageElement
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     * @throws NoSuchAlgorithmException
     */
    OmemoVAxolotlElement processSendingMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                               ArrayList<BareJid> recipients,
                                               Message message)
            throws CryptoFailedException, UndecidedOmemoIdentityException, NoSuchAlgorithmException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException,
            CannotEstablishOmemoSessionException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        CannotEstablishOmemoSessionException sessionException = null;
        // Them - The contact wants to read the message on all their devices.
        HashMap<BareJid, ArrayList<OmemoDevice>> receivers = new HashMap<>();
        for (BareJid recipient : recipients) {
            try {
                buildMissingOmemoSessions(managerGuard, recipient);
            } catch (CannotEstablishOmemoSessionException e) {

                if (sessionException == null) {
                    sessionException = e;
                } else {
                    sessionException.addFailures(e);
                }
            }
        }

        for (BareJid recipient : recipients) {
            CachedDeviceList theirDevices = getOmemoStoreBackend().loadCachedDeviceList(userDevice, recipient);
            ArrayList<OmemoDevice> receivingDevices = new ArrayList<>();
            for (int id : theirDevices.getActiveDevices()) {
                OmemoDevice recipientDevice = new OmemoDevice(recipient, id);

                if (getOmemoStoreBackend().containsRawSession(userDevice, recipientDevice)) {
                    receivingDevices.add(recipientDevice);
                }

                if (sessionException != null) {
                    sessionException.addSuccess(recipientDevice);
                }
            }

            if (!receivingDevices.isEmpty()) {
                receivers.put(recipient, receivingDevices);
            }
        }

        // Us - We want to read the message on all of our devices
        CachedDeviceList ourDevices = getOmemoStoreBackend().loadCachedDeviceList(userDevice, omemoManager.getOwnJid());
        if (ourDevices == null) {
            ourDevices = new CachedDeviceList();
        }

        ArrayList<OmemoDevice> ourReceivingDevices = new ArrayList<>();
        for (int id : ourDevices.getActiveDevices()) {
            OmemoDevice remoteUserDevice = new OmemoDevice(omemoManager.getOwnJid(), id);
            if (id == omemoManager.getDeviceId()) {
                // Don't build session with our exact device.
                continue;
            }

            Date lastReceived = getOmemoStoreBackend().getDateOfLastReceivedMessage(userDevice, remoteUserDevice);
            if (lastReceived == null) {
                getOmemoStoreBackend().setDateOfLastReceivedMessage(userDevice, remoteUserDevice, new Date());
                lastReceived = new Date();
            }

            if (OmemoConfiguration.getIgnoreStaleDevices() && System.currentTimeMillis() - lastReceived.getTime()
                    > 1000L * 60 * 60 * OmemoConfiguration.getIgnoreStaleDevicesAfterHours()) {
                LOGGER.log(Level.WARNING, "Refusing to encrypt message for stale device " + remoteUserDevice +
                        " which was inactive for at least " + OmemoConfiguration.getIgnoreStaleDevicesAfterHours() +
                        " hours.");
            } else {

                if (getOmemoStoreBackend().containsRawSession(userDevice, remoteUserDevice)) {
                    ourReceivingDevices.add(remoteUserDevice);
                }
            }
        }

        if (!ourReceivingDevices.isEmpty()) {
            receivers.put(omemoManager.getOwnJid(), ourReceivingDevices);
        }

        if (sessionException != null && sessionException.requiresThrowing()) {
            throw sessionException;
        }

        return encryptOmemoMessage(managerGuard, receivers, message);
    }

    /**
     * Decrypt a incoming OmemoMessageElement that was sent by the OmemoDevice 'from'.
     *
     * @param managerGuard omemoManager of the decrypting device.
     * @param from          OmemoDevice that sent the message
     * @param message       Encrypted OmemoMessageElement
     * @param information   OmemoMessageInformation object which will contain metadata about the encryption
     * @return Decrypted message
     * @throws CryptoFailedException when decrypting message fails for some reason
     * @throws InterruptedException
     * @throws CorruptedOmemoKeyException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     * @throws NoRawSessionException
     */
    private Message decryptOmemoMessageElement(OmemoManager.LoggedInOmemoManager managerGuard,
                                               OmemoDevice from,
                                               OmemoElement message,
                                               final OmemoMessageInformation information)
            throws CryptoFailedException, InterruptedException, CorruptedOmemoKeyException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException,
            NoRawSessionException
    {
        CipherAndAuthTag transportedKey = decryptTransportedOmemoKey(managerGuard, from, message, information);
        return OmemoRatchet.decryptMessageElement(message, transportedKey);
    }

    /**
     * Decrypt a messageKey that was transported in an OmemoElement.
     *
     * @param managerGuard  omemoManager of the receiving device.
     * @param sender        omemoDevice of the sender.
     * @param omemoMessage  omemoElement containing the key.
     * @param messageInfo   omemoMessageInformation that will contain metadata about the encryption.
     * @return a CipherAndAuthTag pair
     * @throws CryptoFailedException
     * @throws NoRawSessionException
     * @throws InterruptedException
     * @throws CorruptedOmemoKeyException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    private CipherAndAuthTag decryptTransportedOmemoKey(OmemoManager.LoggedInOmemoManager managerGuard,
                                                        OmemoDevice  sender,
                                                        OmemoElement omemoMessage,
                                                        OmemoMessageInformation messageInfo)
            throws CryptoFailedException, NoRawSessionException, InterruptedException, CorruptedOmemoKeyException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException, SmackException.NoResponseException
    {
        OmemoManager omemoManager = managerGuard.get();
        OmemoDevice userDevice = omemoManager.getOwnDevice();

        int preKeyCountBefore = getOmemoStoreBackend().loadOmemoPreKeys(userDevice).size();

        CipherAndAuthTag cipherAndAuthTag = omemoRatchets.get(managerGuard.get()).retrieveMessageKeyAndAuthTag(sender, omemoMessage);

        messageInfo.setSenderDevice(sender);
        messageInfo.setSenderFingerprint(omemoStore.keyUtil().getFingerprintOfIdentityKey(
                omemoStore.loadOmemoIdentityKey(userDevice, sender)));

        if (preKeyCountBefore != getOmemoStoreBackend().loadOmemoPreKeys(userDevice).size()) {
            LOGGER.log(Level.FINE, "We used up a preKey. Publish new Bundle.");
            publishBundle(managerGuard);
        }
        return cipherAndAuthTag;
    }

    /**
     * Encrypt the message and return it as an OmemoMessageElement.
     *
     * @param managerGuard omemoManager of the encrypting device.
     * @param recipients List of devices that will be able to decipher the message.
     * @param message   Clear text message
     *
     * @throws CryptoFailedException when some cryptographic function fails
     * @throws UndecidedOmemoIdentityException when the identity of one or more contacts is undecided
     *
     * @return OmemoMessageElement
     */
    OmemoVAxolotlElement encryptOmemoMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                             HashMap<BareJid, ArrayList<OmemoDevice>> recipients,
                                             Message message)
            throws CryptoFailedException, UndecidedOmemoIdentityException
    {
        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                builder;
        try {
            builder = new OmemoMessageBuilder<>(managerGuard, getOmemoRatchet(managerGuard.get()), message.getBody());
        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException |
                NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException(e);
        }

        UndecidedOmemoIdentityException undecided = null;

        for (Map.Entry<BareJid, ArrayList<OmemoDevice>> entry : recipients.entrySet()) {
            for (OmemoDevice c : entry.getValue()) {
                try {
                    builder.addRecipient(c);
                } catch (CorruptedOmemoKeyException | CannotEstablishOmemoSessionException e) {
                    // TODO: How to react?
                    LOGGER.log(Level.SEVERE, "encryptOmemoMessage failed to establish a session with device "
                            + c + ": " + e.getMessage());
                } catch (UndecidedOmemoIdentityException e) {
                    // Collect all undecided devices
                    if (undecided == null) {
                        undecided = e;
                    } else {
                        undecided.join(e);
                    }
                }
            }
        }

        if (undecided != null) {
            throw undecided;
        }

        return builder.finish();
    }

    /**
     * Prepares a keyTransportElement with a random aes key and iv.
     *
     * @param managerGuard omemoManager of the sending device.
     * @param recipients recipients of the omemoKeyTransportElement
     * @return KeyTransportElement
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     * @throws CorruptedOmemoKeyException
     * @throws CannotEstablishOmemoSessionException
     */
    OmemoVAxolotlElement prepareOmemoKeyTransportElement(OmemoManager.LoggedInOmemoManager managerGuard,
                                                         OmemoDevice... recipients)
            throws CryptoFailedException, UndecidedOmemoIdentityException, CorruptedOmemoKeyException,
            CannotEstablishOmemoSessionException
    {
        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                builder;
        try {
            builder = new OmemoMessageBuilder<>(managerGuard, getOmemoRatchet(managerGuard.get()), null);

        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException |
                NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException(e);
        }

        for (OmemoDevice r : recipients) {
            builder.addRecipient(r);
        }

        return builder.finish();
    }

    /**
     * Prepare a KeyTransportElement with aesKey and iv.
     *
     * @param managerGuard  OmemoManager of the sending device.
     * @param aesKey        AES key
     * @param iv            initialization vector
     * @param recipients    recipients
     * @return              KeyTransportElement
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     * @throws CorruptedOmemoKeyException
     * @throws CannotEstablishOmemoSessionException
     */
    OmemoVAxolotlElement prepareOmemoKeyTransportElement(OmemoManager.LoggedInOmemoManager managerGuard,
                                                         byte[] aesKey,
                                                         byte[] iv,
                                                         OmemoDevice... recipients)
            throws CryptoFailedException, UndecidedOmemoIdentityException, CorruptedOmemoKeyException,
            CannotEstablishOmemoSessionException
    {
        OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                builder;
        try {
            builder = new OmemoMessageBuilder<>(managerGuard, getOmemoRatchet(managerGuard.get()), aesKey, iv);

        } catch (UnsupportedEncodingException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException |
                NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new CryptoFailedException(e);
        }

        for (OmemoDevice r : recipients) {
            builder.addRecipient(r);
        }

        return builder.finish();
    }

    /**
     * Return a new RatchetUpdateMessage.
     *
     * @param managerGuard  omemoManager of the sending device.
     * @param recipient     recipient
     * @param preKeyMessage if true, a new session will be built for this message (useful to repair broken sessions)
     *                      otherwise the message will be encrypted using the existing session.
     * @return              OmemoRatchetUpdateMessage
     * @throws CannotEstablishOmemoSessionException
     * @throws CorruptedOmemoKeyException
     * @throws CryptoFailedException
     * @throws UndecidedOmemoIdentityException
     */
    protected Message getOmemoRatchetUpdateMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                                   OmemoDevice recipient,
                                                   boolean preKeyMessage)
            throws CannotEstablishOmemoSessionException, CorruptedOmemoKeyException, CryptoFailedException,
            UndecidedOmemoIdentityException
    {
        if (preKeyMessage) {
            buildSessionWithDevice(managerGuard, recipient, true);
        }

        OmemoVAxolotlElement keyTransportElement = prepareOmemoKeyTransportElement(managerGuard, recipient);
        Message ratchetUpdateMessage = managerGuard.get().finishMessage(keyTransportElement);
        ratchetUpdateMessage.setTo(recipient.getJid());

        return ratchetUpdateMessage;
    }

    /**
     * Send an OmemoRatchetUpdateMessage to recipient. If preKeyMessage is true, the message will be encrypted using a
     * freshly built session. This can be used to repair broken sessions.
     *
     * @param managerGuard      omemoManager of the sending device.
     * @param recipient         recipient
     * @param preKeyMessage     shall this be a preKeyMessage?
     * @throws UndecidedOmemoIdentityException
     * @throws CorruptedOmemoKeyException
     * @throws CryptoFailedException
     * @throws CannotEstablishOmemoSessionException
     */
    protected void sendOmemoRatchetUpdateMessage(OmemoManager.LoggedInOmemoManager managerGuard,
                                                 OmemoDevice recipient,
                                                 boolean preKeyMessage)
            throws UndecidedOmemoIdentityException, CorruptedOmemoKeyException, CryptoFailedException,
            CannotEstablishOmemoSessionException
    {
        Message ratchetUpdateMessage = getOmemoRatchetUpdateMessage(managerGuard, recipient, preKeyMessage);

        try {
            managerGuard.get().getConnection().sendStanza(ratchetUpdateMessage);

        } catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "sendOmemoRatchetUpdateMessage failed: " + e.getMessage());
        }
    }

    /**
     * Try to decrypt a mamQueryResult. Note that OMEMO messages can only be decrypted once on a device, so if you
     * try to decrypt a message that has been decrypted earlier in time, the decryption will fail. You should handle
     * message history locally when using OMEMO, since you cannot rely on MAM.
     *
     * @param managerGuard omemoManager of the decrypting device.
     * @param mamQueryResult mamQueryResult that shall be decrypted.
     * @return list of decrypted messages.
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    List<ClearTextMessage> decryptMamQueryResult(OmemoManager.LoggedInOmemoManager managerGuard,
                                                 MamManager.MamQueryResult mamQueryResult)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException
    {
        List<ClearTextMessage> result = new ArrayList<>();
        for (Forwarded f : mamQueryResult.forwardedMessages) {
            if (OmemoManager.stanzaContainsOmemoElement(f.getForwardedStanza())) {
                // Decrypt OMEMO messages
                try {
                    result.add(processLocalMessage(managerGuard, f.getForwardedStanza().getFrom().asBareJid(),
                            (Message) f.getForwardedStanza()));
                } catch (NoRawSessionException | CorruptedOmemoKeyException | CryptoFailedException e) {
                    LOGGER.log(Level.WARNING, "decryptMamQueryResult failed to decrypt message from "
                            + f.getForwardedStanza().getFrom() + " due to corrupted session/key: " + e.getMessage());
                }
            } else {
                // Wrap cleartext messages
                Message m = (Message) f.getForwardedStanza();
                result.add(new ClearTextMessage(m.getBody(), m,
                        new OmemoMessageInformation(null, null,
                                OmemoMessageInformation.CARBON.NONE, false)));
            }
        }
        return result;
    }

    /**
     * Return the barejid of the user that sent the message inside the MUC. If the message wasn't sent in a MUC,
     * return null;
     *
     * @param managerGuard omemoManager
     * @param stanza message
     * @return BareJid of the sender.
     */
    private static OmemoDevice getSender(OmemoManager.LoggedInOmemoManager managerGuard,
                                         Stanza stanza) {
        OmemoElement omemoElement = stanza.getExtension(OmemoElement.ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
        Jid sender = stanza.getFrom();
        if (isMucMessage(managerGuard, stanza)) {
            MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(managerGuard.get().getConnection());
            MultiUserChat muc = mucm.getMultiUserChat(sender.asEntityBareJidIfPossible());
            sender = muc.getOccupant(sender.asEntityFullJidIfPossible()).getJid().asBareJid();
        }
        if (sender == null) {
            throw new AssertionError("Sender is null.");
        }
        return new OmemoDevice(sender.asBareJid(), omemoElement.getHeader().getSid());
    }

    /**
     * Return true, if the user knows a multiUserChat with a jid matching the sender of the stanza.
     * @param managerGuard  omemoManager of the user
     * @param stanza        stanza in question
     * @return              true if MUC message, otherwise false.
     */
    private static boolean isMucMessage(OmemoManager.LoggedInOmemoManager managerGuard, Stanza stanza) {
        BareJid sender = stanza.getFrom().asBareJid();
        MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(managerGuard.get().getConnection());

        return mucm.getJoinedRooms().contains(sender.asEntityBareJidIfPossible());
    }

    @Override
    public void onOmemoMessageStanzaReceived(Stanza stanza, OmemoManager.LoggedInOmemoManager managerGuard) {
        OmemoManager omemoManager = managerGuard.get();
        Message decrypted;
        OmemoElement omemoMessage = stanza.getExtension(OmemoElement.ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
        OmemoMessageInformation messageInfo = new OmemoMessageInformation();
        MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(omemoManager.getConnection());
        OmemoDevice senderDevice = getSender(managerGuard, stanza);
        try {
            // Is it a MUC message...
            if (isMucMessage(managerGuard, stanza)) {

                MultiUserChat muc = mucm.getMultiUserChat(stanza.getFrom().asEntityBareJidIfPossible());
                if (omemoMessage.isMessageElement()) {

                    decrypted = processReceivingMessage(managerGuard, senderDevice, omemoMessage, messageInfo);
                    if (decrypted != null) {
                        omemoManager.notifyOmemoMucMessageReceived(muc, senderDevice.getJid(), decrypted.getBody(),
                                (Message) stanza, null, messageInfo);
                    }

                } else if (omemoMessage.isKeyTransportElement()) {

                    CipherAndAuthTag cipherAndAuthTag = decryptTransportedOmemoKey(managerGuard, senderDevice,
                            omemoMessage, messageInfo);
                    if (cipherAndAuthTag != null) {
                        omemoManager.notifyOmemoMucKeyTransportMessageReceived(muc, senderDevice.getJid(), cipherAndAuthTag,
                                (Message) stanza, null, messageInfo);
                    }
                }
            }
            // ... or a normal chat message...
            else {
                if (omemoMessage.isMessageElement()) {

                    decrypted = processReceivingMessage(managerGuard, senderDevice, omemoMessage, messageInfo);
                    if (decrypted != null) {
                        omemoManager.notifyOmemoMessageReceived(decrypted.getBody(), (Message) stanza, null,
                                messageInfo);
                    }

                } else if (omemoMessage.isKeyTransportElement()) {

                    CipherAndAuthTag cipherAndAuthTag = decryptTransportedOmemoKey(managerGuard, senderDevice, omemoMessage,
                            messageInfo);
                    if (cipherAndAuthTag != null) {
                        omemoManager.notifyOmemoKeyTransportMessageReceived(cipherAndAuthTag, (Message) stanza,
                                null, messageInfo);
                    }
                }
            }

        } catch (CryptoFailedException | CorruptedOmemoKeyException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {

            LOGGER.log(Level.WARNING, "internal omemoMessageListener failed to decrypt incoming OMEMO message: "
                    + e.getMessage());

        } catch (NoRawSessionException e) {
            try {
                LOGGER.log(Level.INFO, "Received message with invalid session from " +
                        senderDevice + ". Send RatchetUpdateMessage.");
                sendOmemoRatchetUpdateMessage(managerGuard, senderDevice, true);

            } catch (UndecidedOmemoIdentityException | CorruptedOmemoKeyException | CannotEstablishOmemoSessionException
                    | CryptoFailedException e1) {

                LOGGER.log(Level.WARNING, "internal omemoMessageListener failed to establish a session for incoming OMEMO message: "
                        + e.getMessage());
            }
        }
    }

    @Override
    public void onOmemoCarbonCopyReceived(CarbonExtension.Direction direction,
                                          Message carbonCopy,
                                          Message wrappingMessage,
                                          final OmemoManager.LoggedInOmemoManager managerGuard)
    {
        OmemoManager omemoManager = managerGuard.get();

        final OmemoDevice senderDevice = getSender(managerGuard, carbonCopy);
        Message decrypted;
        MultiUserChatManager mucm = MultiUserChatManager.getInstanceFor(omemoManager.getConnection());
        OmemoElement omemoMessage = carbonCopy.getExtension(OmemoElement.ENCRYPTED, OMEMO_NAMESPACE_V_AXOLOTL);
        OmemoMessageInformation messageInfo = new OmemoMessageInformation();

        if (CarbonExtension.Direction.received.equals(direction)) {
            messageInfo.setCarbon(OmemoMessageInformation.CARBON.RECV);
        } else {
            messageInfo.setCarbon(OmemoMessageInformation.CARBON.SENT);
        }

        try {
            // Is it a MUC message...
            if (isMucMessage(managerGuard, carbonCopy)) {

                MultiUserChat muc = mucm.getMultiUserChat(carbonCopy.getFrom().asEntityBareJidIfPossible());
                if (omemoMessage.isMessageElement()) {

                    decrypted = processReceivingMessage(managerGuard, senderDevice, omemoMessage, messageInfo);
                    if (decrypted != null) {
                        omemoManager.notifyOmemoMucMessageReceived(muc, senderDevice.getJid(), decrypted.getBody(),
                                carbonCopy, wrappingMessage, messageInfo);
                    }

                } else if (omemoMessage.isKeyTransportElement()) {

                    CipherAndAuthTag cipherAndAuthTag = decryptTransportedOmemoKey(managerGuard, senderDevice,
                            omemoMessage, messageInfo);
                    if (cipherAndAuthTag != null) {
                        omemoManager.notifyOmemoMucKeyTransportMessageReceived(muc, senderDevice.getJid(), cipherAndAuthTag,
                                carbonCopy, wrappingMessage, messageInfo);
                    }
                }
            }
            // ... or a normal chat message...
            else {
                if (omemoMessage.isMessageElement()) {

                    decrypted = processReceivingMessage(managerGuard, senderDevice, omemoMessage, messageInfo);
                    if (decrypted != null) {
                        omemoManager.notifyOmemoMessageReceived(decrypted.getBody(), carbonCopy, wrappingMessage, messageInfo);
                    }

                } else if (omemoMessage.isKeyTransportElement()) {

                    CipherAndAuthTag cipherAndAuthTag = decryptTransportedOmemoKey(managerGuard, senderDevice,
                            omemoMessage, messageInfo);
                    if (cipherAndAuthTag != null) {
                        omemoManager.notifyOmemoKeyTransportMessageReceived(cipherAndAuthTag, carbonCopy,
                                null, messageInfo);
                    }
                }
            }

        } catch (CryptoFailedException | CorruptedOmemoKeyException | InterruptedException |
                SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NoResponseException e) {
            LOGGER.log(Level.WARNING, "internal omemoMessageListener failed to decrypt incoming OMEMO carbon copy: "
                    + e.getMessage());

        } catch (final NoRawSessionException e) {
            Async.go(new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.log(Level.INFO, "Received OMEMO carbon copy message with invalid session from " +
                                senderDevice + ". Send RatchetUpdateMessage.");
                        sendOmemoRatchetUpdateMessage(managerGuard, senderDevice, true);

                    } catch (UndecidedOmemoIdentityException | CorruptedOmemoKeyException |
                            CannotEstablishOmemoSessionException | CryptoFailedException e1) {
                        LOGGER.log(Level.WARNING, "internal omemoMessageListener failed to establish a session for incoming OMEMO carbon message: "
                                + e.getMessage());
                    }
                }
            });

        }
    }

    protected abstract OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    instantiateOmemoRatchet(OmemoManager manager,
                            OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store);

    protected OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoRatchet(OmemoManager manager) {
        OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
                omemoRatchet = omemoRatchets.get(manager);
        if (omemoRatchet == null) {
            omemoRatchet = instantiateOmemoRatchet(manager, omemoStore);
            omemoRatchets.put(manager, omemoRatchet);
        }
        return omemoRatchet;
    }
}
