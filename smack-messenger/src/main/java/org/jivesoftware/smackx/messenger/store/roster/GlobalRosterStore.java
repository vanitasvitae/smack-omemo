package org.jivesoftware.smackx.messenger.store.roster;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jivesoftware.smack.roster.packet.RosterPacket;

import org.jxmpp.jid.Jid;

public interface GlobalRosterStore {

    List<RosterPacket.Item> getEntries(UUID accountId);

    RosterPacket.Item getEntry(UUID accountId, Jid bareJid);

    String getRosterVersion(UUID accountId);

    boolean addEntry(UUID accountId, RosterPacket.Item item, String version);

    boolean resetEntries(UUID accountId, Collection<RosterPacket.Item> items, String version);

    boolean removeEntry(UUID accountId, Jid bareJid, String version);

    void resetStore(UUID accountId);
}
