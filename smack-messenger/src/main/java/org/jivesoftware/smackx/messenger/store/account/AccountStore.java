package org.jivesoftware.smackx.messenger.store.account;

import java.util.List;
import java.util.UUID;

import org.jivesoftware.smackx.messenger.AccountRecord;

public interface AccountStore {

    List<AccountRecord> getAllAccounts();
    AccountRecord getAccount(UUID accountId);

}
