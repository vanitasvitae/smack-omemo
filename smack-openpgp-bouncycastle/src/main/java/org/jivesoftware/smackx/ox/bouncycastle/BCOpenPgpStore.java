package org.jivesoftware.smackx.ox.bouncycastle;

import org.jivesoftware.smackx.ox.OpenPgpStore;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;

public interface BCOpenPgpStore extends OpenPgpStore {

    KeyringConfig getKeyringConfig();
}
