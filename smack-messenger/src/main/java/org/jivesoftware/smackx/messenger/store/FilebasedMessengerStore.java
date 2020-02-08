package org.jivesoftware.smackx.messenger.store;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.rosterstore.DirectoryRosterStore;
import org.jivesoftware.smackx.caps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.messenger.AccountRecord;

import org.jxmpp.jid.Jid;

public class FilebasedMessengerStore implements MessengerStore {

    private final File storeBaseDir;
    private final Map<UUID, DirectoryRosterStore> rosterStoreMap = new ConcurrentHashMap<>();
    private final SimpleDirectoryPersistentCache entityCapsCache;
    private final Map<UUID, AccountRecord> accounts = new ConcurrentHashMap<>();

    public FilebasedMessengerStore(File storeBaseDir) {
        this.storeBaseDir = storeBaseDir;
        entityCapsCache = new SimpleDirectoryPersistentCache(new File(storeBaseDir, "entityCaps"));
    }

    private DirectoryRosterStore getRosterStore(UUID accountId) {
        DirectoryRosterStore store = rosterStoreMap.get(accountId);
        if (store == null) {
            File accountDir = new File(storeBaseDir, accountId.toString());
            File rosterDir = new File(accountDir, "roster");
            store = DirectoryRosterStore.open(rosterDir);
            if (store == null) {
                store = DirectoryRosterStore.init(rosterDir);
            }
            rosterStoreMap.put(accountId, store);
        }
        return store;
    }


    @Override
    public void addDiscoverInfoByNodePersistent(String nodeVer, DiscoverInfo info) {
        entityCapsCache.addDiscoverInfoByNodePersistent(nodeVer, info);
    }

    @Override
    public DiscoverInfo lookup(String nodeVer) {
        return entityCapsCache.lookup(nodeVer);
    }

    @Override
    public void emptyCache() {
        entityCapsCache.emptyCache();
    }

    @Override
    public List<AccountRecord> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    @Override
    public AccountRecord getAccount(UUID accountId) {
        return accounts.get(accountId);
    }

    @Override
    public List<RosterPacket.Item> getEntries(UUID accountId) {
        return getRosterStore(accountId).getEntries();
    }

    @Override
    public RosterPacket.Item getEntry(UUID accountId, Jid bareJid) {
        return getRosterStore(accountId).getEntry(bareJid);
    }

    @Override
    public String getRosterVersion(UUID accountId) {
        return getRosterStore(accountId).getRosterVersion();
    }

    @Override
    public boolean addEntry(UUID accountId, RosterPacket.Item item, String version) {
        return getRosterStore(accountId).addEntry(item, version);
    }

    @Override
    public boolean resetEntries(UUID accountId, Collection<RosterPacket.Item> items, String version) {
        return getRosterStore(accountId).resetEntries(items, version);
    }

    @Override
    public boolean removeEntry(UUID accountId, Jid bareJid, String version) {
        return getRosterStore(accountId).removeEntry(bareJid, version);
    }

    @Override
    public void resetStore(UUID accountId) {
        getRosterStore(accountId).resetStore();
    }
}
