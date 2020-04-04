package org.jivesoftware.smackx.mix.core;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

import org.jxmpp.jid.DomainBareJid;

public class MixService {

    private final DomainBareJid serviceJid;

    private boolean isSearchable;

    private boolean isSupportingChannelCreation;

    public MixService(DomainBareJid serviceJid, DiscoverInfo serviceInfo) {
        this.serviceJid = serviceJid;

        this.isSearchable = discoverIsSearchable(serviceInfo);
        this.isSupportingChannelCreation = discoverIsSupportingChannelCreation(serviceInfo);
    }

    public boolean isSearchable() {
        return isSearchable;
    }

    public boolean isSupportingChannelCreation() {
        return isSupportingChannelCreation;
    }

    private boolean discoverIsSupportingChannelCreation(DiscoverInfo serviceInfo) {
        return serviceInfo.containsFeature(MixCoreConstants.FEATURE_CREATE_CHANNEL_1);
    }

    private boolean discoverIsSearchable(DiscoverInfo serviceInfo) {
        return serviceInfo.containsFeature(MixCoreConstants.FEATURE_SEARCHABLE_1);
    }
}
