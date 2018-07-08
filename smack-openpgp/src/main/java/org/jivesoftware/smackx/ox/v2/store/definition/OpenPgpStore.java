package org.jivesoftware.smackx.ox.v2.store.definition;

import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;

import org.jxmpp.jid.BareJid;
import org.pgpainless.pgpainless.key.protection.SecretKeyRingProtector;

public interface OpenPgpStore extends OpenPgpKeyStore, OpenPgpMetadataStore, OpenPgpTrustStore {

    OpenPgpContact getOpenPgpContact(BareJid contactsJid);

    void setKeyRingProtector(SecretKeyRingProtector unlocker);

    SecretKeyRingProtector getKeyRingProtector();

    void setSecretKeyPassphraseCallback(SecretKeyPassphraseCallback callback);

}
