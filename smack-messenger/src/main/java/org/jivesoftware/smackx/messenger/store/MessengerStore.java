package org.jivesoftware.smackx.messenger.store;

import org.jivesoftware.smackx.caps.cache.EntityCapsPersistentCache;
import org.jivesoftware.smackx.messenger.store.account.AccountStore;
import org.jivesoftware.smackx.messenger.store.roster.GlobalRosterStore;

public interface MessengerStore extends EntityCapsPersistentCache, AccountStore, GlobalRosterStore {

}
