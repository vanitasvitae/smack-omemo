/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.mix.core;

import static org.jivesoftware.smackx.mix.core.MixCoreConstants.FEATURE_CORE_1;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mix.core.element.iq.ClientJoinIQ;
import org.jivesoftware.smackx.mix.core.element.iq.ClientJoinIQBuilder;
import org.jivesoftware.smackx.mix.core.exception.NotAMixChannelOrNoPermissionToSubscribeException;
import org.jivesoftware.smackx.mix.core.exception.NotAMixServiceException;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.cache.ExpirationCache;

public final class MixManager extends Manager {

    private static final Map<XMPPConnection, MixManager> INSTANCES = new WeakHashMap<>();
    private static final ExpirationCache<DomainBareJid, MixService> KNOWN_MIX_SERVICES =
            new ExpirationCache<>(100, 1000 * 60 * 60 * 24);

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                MixManager.getInstanceFor(connection);
            }
        });
    }

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    public static MixManager getInstanceFor(XMPPConnection connection) {
        MixManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new MixManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private MixManager(XMPPConnection connection) {
        super(connection);
        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection());
        serviceDiscoveryManager.addFeature(FEATURE_CORE_1);
    }

    public List<EntityBareJid> discoverMixChannels(DomainBareJid mixServiceAddress)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, NotAMixServiceException,
            SmackException.FeatureNotSupportedException {
        MixService mixService = getOrDiscoverMixService(mixServiceAddress);

        if (!mixService.isSearchable()) {
            throw new SmackException.FeatureNotSupportedException(MixCoreConstants.FEATURE_SEARCHABLE_1, mixServiceAddress);
        }

        DiscoverItems discoverItems = serviceDiscoveryManager.discoverItems(mixServiceAddress);
        List<EntityBareJid> channelJids = discoverItems.getItems().stream()
                .map(DiscoverItems.Item::getEntityID)
                .map(Jid::asEntityBareJidOrThrow)
                .collect(Collectors.toList());
        return channelJids;
    }

    private MixService getOrDiscoverMixService(DomainBareJid mixServiceAddress)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, NotAMixServiceException {
        if (KNOWN_MIX_SERVICES.containsKey(mixServiceAddress)) {
            return KNOWN_MIX_SERVICES.get(mixServiceAddress);
        }
        return discoverMixService(mixServiceAddress);
    }

    private MixService discoverMixService(DomainBareJid mixServiceAddress)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, NotAMixServiceException {
        DiscoverInfo mixServiceDiscoverInfo = serviceDiscoveryManager.discoverInfo(mixServiceAddress);

        if (!mixServiceDiscoverInfo.hasIdentity("conference", "mix")) {
            throw new NotAMixServiceException("Identity of the service MUST have a category 'conference' and a type of 'mix'.");
        }
        if (!mixServiceDiscoverInfo.containsFeature(FEATURE_CORE_1)) {
            throw new NotAMixServiceException("Service MUST include the '" + FEATURE_CORE_1 + "' feature.");
        }

        mixServiceStandardComplianceChecks(mixServiceDiscoverInfo);

        MixService mixService = new MixService(mixServiceAddress, mixServiceDiscoverInfo);

        KNOWN_MIX_SERVICES.put(mixServiceAddress, mixService);

        return mixService;
    }

    public MixChannel discoverMixChannelInformation(EntityBareJid mixChannelAddress)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, NotAMixChannelOrNoPermissionToSubscribeException {
        DiscoverInfo channelInfo = serviceDiscoveryManager.discoverInfo(mixChannelAddress);

        if (!channelInfo.hasIdentity("conference", "mix")) {
            throw new NotAMixChannelOrNoPermissionToSubscribeException(
                    "DiscoverInfo did not contain identity with category 'conference' and type 'mix'.");
        }

        mixChannelStandardComplianceChecks(channelInfo);

        //return new MixChannel(mixChannelAddress, channelInfo);
        return null;
    }

    /**
     * Perform checks for standards compliance on the service.
     * These checks are not strictly necessary, but may be used to identify faulty server implementations.
     *
     * @param info DiscoverInfo of the mix service.
     */
    private void mixServiceStandardComplianceChecks(DiscoverInfo info) {
        if (info.containsFeature(MamElements.NAMESPACE)) {
            // XEP-0369: §6.1
            throw new AssertionError("A MIX service MUST NOT advertise support for MAM.");
        }
        if (info.containsFeature(PubSub.NAMESPACE)) {
            // XEP-0369: §6.1
            throw new AssertionError("A MIX service MUST NOT advertise support for PubSub.");
        }
    }

    private void mixChannelStandardComplianceChecks(DiscoverInfo channelInfo) throws NotAMixChannelOrNoPermissionToSubscribeException {
        if (!channelInfo.containsFeature(FEATURE_CORE_1)) {
            throw new NotAMixChannelOrNoPermissionToSubscribeException(
                    "MIX channel did not advertise feature '" + FEATURE_CORE_1 + "'.");
        }
        if (!channelInfo.containsFeature(MamElements.NAMESPACE)) {
            throw new NotAMixChannelOrNoPermissionToSubscribeException(
                    "MIX channel did not advertise feature '" + MamElements.NAMESPACE + "'.");
        }
    }

    public MixChannel join(EntityBareJid channelAddress, String nick)
            throws XMPPException.XMPPErrorException, NotAMixChannelOrNoPermissionToSubscribeException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        MixChannel channel = discoverMixChannelInformation(channelAddress);
        ClientJoinIQ iq = new ClientJoinIQBuilder(connection())
                .setNickname(nick)
                .setChannelAddress(channelAddress)
                .addMixNodeSubscription(MixNodes.NODE_MESSAGES)
                .addMixNodeSubscription(MixNodes.NODE_PRESENCE)
                .build();
        return null;
    }
}
