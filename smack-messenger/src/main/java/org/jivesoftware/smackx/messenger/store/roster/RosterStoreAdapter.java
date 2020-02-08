package org.jivesoftware.smackx.messenger.store.roster;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.rosterstore.RosterStore;
import org.jivesoftware.smackx.messenger.store.roster.GlobalRosterStore;

import org.jxmpp.jid.Jid;

public class RosterStoreAdapter implements RosterStore {

    private final GlobalRosterStore store;
    private final UUID accountId;

    public RosterStoreAdapter(UUID accountId, GlobalRosterStore globalRosterStore) {
        this.store = globalRosterStore;
        this.accountId = accountId;
    }

    @Override
    public List<RosterPacket.Item> getEntries() {
        return store.getEntries(accountId);
    }

    @Override
    public RosterPacket.Item getEntry(Jid bareJid) {
        return store.getEntry(accountId, bareJid);
    }

    @Override
    public String getRosterVersion() {
        return store.getRosterVersion(accountId);
    }

    @Override
    public boolean addEntry(RosterPacket.Item item, String version) {
        return store.addEntry(accountId, item, version);
    }

    @Override
    public boolean resetEntries(Collection<RosterPacket.Item> items, String version) {
        return store.resetEntries(accountId, items, version);
    }

    @Override
    public boolean removeEntry(Jid bareJid, String version) {
        return store.removeEntry(accountId, bareJid, version);
    }

    @Override
    public void resetStore() {
        store.resetStore(accountId);
    }
}
