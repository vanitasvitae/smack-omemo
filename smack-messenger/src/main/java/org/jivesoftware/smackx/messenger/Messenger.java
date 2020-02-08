package org.jivesoftware.smackx.messenger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.csi.ClientStateIndicationManager;
import org.jivesoftware.smackx.iqversion.VersionManager;
import org.jivesoftware.smackx.messenger.connection.ConnectionFactory;
import org.jivesoftware.smackx.messenger.connection.XmppTcpConnectionFactory;
import org.jivesoftware.smackx.messenger.csi.ClientStateListener;
import org.jivesoftware.smackx.messenger.store.MessengerStore;
import org.jivesoftware.smackx.messenger.store.roster.RosterStoreAdapter;
import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;

import org.jxmpp.stringprep.XmppStringprepException;

public class Messenger implements ClientStateListener {

    private final Map<UUID, XmppAccount> accounts = new ConcurrentHashMap<>();
    private final MessengerStore messengerStore;

    private ConnectionFactory connectionFactory = new XmppTcpConnectionFactory();

    public Messenger(MessengerStore store) {
        this.messengerStore = store;
        EntityCapsManager.setPersistentCache(store);

        setGlobalDefaults();
    }

    private void setGlobalDefaults() {
        ReconnectionManager.setEnabledPerDefault(true);
        StableUniqueStanzaIdManager.setEnabledByDefault(true);
        VersionManager.setAutoAppendSmackVersion(false);
    }

    public XmppAccount addAccount(UUID accountId, String username, String password, String serviceName)
            throws XmppStringprepException {
        XMPPConnection connection = connectionFactory.createConnection(username, password, serviceName);

        XmppAccount xmppAccount = new XmppAccount(accountId, connection);
        accounts.put(accountId, xmppAccount);

        offlineAccountSetup(xmppAccount);

        return xmppAccount;
    }

    private void offlineAccountSetup(XmppAccount account) {
        Roster.getInstanceFor(account.getConnection()).setRosterStore(
                new RosterStoreAdapter(account.getAccountId(), messengerStore));
    }

    private void onlineAccountSetup(XmppAccount account)
            throws InterruptedException, XMPPException, SmackException {
        if (CarbonManager.getInstanceFor(account.getConnection()).isSupportedByServer()) {
            CarbonManager.getInstanceFor(account.getConnection()).enableCarbons();
        }

        
    }

    @Override
    public synchronized void onClientInForeground() {
        for (XmppAccount connection : accounts.values()) {
            trySetCsiActive(connection);
        }
    }

    private void trySetCsiActive(XmppAccount connection) {
        try {
            ClientStateIndicationManager.active(connection.getConnection());
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void onClientInBackground() {
        for (XmppAccount connection : accounts.values()) {
            trySetCsiInactive(connection);
        }
    }

    private void trySetCsiInactive(XmppAccount connection) {
        try {
            ClientStateIndicationManager.inactive(connection.getConnection());
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
