package org.jivesoftware.smackx.messenger;

import java.util.UUID;

public class AccountRecord {

    private final UUID accountId;
    private final String username;
    private final String password;
    private final String serviceName;

    public AccountRecord(UUID accountId, String username, String password, String serviceName) {
        this.accountId = accountId;
        this.username = username;
        this.password = password;
        this.serviceName = serviceName;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServiceName() {
        return serviceName;
    }
}
