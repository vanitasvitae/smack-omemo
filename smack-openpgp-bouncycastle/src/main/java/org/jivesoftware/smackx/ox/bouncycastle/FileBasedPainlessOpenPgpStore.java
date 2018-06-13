package org.jivesoftware.smackx.ox.bouncycastle;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smackx.ox.OpenPgpStore;
import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyPairException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpPublicKeyException;
import org.jivesoftware.smackx.ox.exception.SmackOpenPgpException;

import org.jxmpp.jid.BareJid;

public class FileBasedPainlessOpenPgpStore implements OpenPgpStore, FileBasedOpenPgpStore {

    @Override
    public OpenPgpV4Fingerprint getPrimaryOpenPgpKeyPairFingerprint() {
        return null;
    }

    @Override
    public void setPrimaryOpenPgpKeyPairFingerprint(OpenPgpV4Fingerprint fingerprint) {

    }

    @Override
    public Set<OpenPgpV4Fingerprint> getAvailableKeyPairFingerprints() {
        return null;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAvailableKeysFingerprints(BareJid contact) throws SmackOpenPgpException {
        return null;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAnnouncedKeysFingerprints(BareJid contact) {
        return null;
    }

    @Override
    public void setAnnouncedKeysFingerprints(BareJid contact, Map<OpenPgpV4Fingerprint, Date> fingerprints) {

    }

    @Override
    public Date getPubkeysLastRevision(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        return null;
    }

    @Override
    public void setPubkeysLastRevision(BareJid owner, OpenPgpV4Fingerprint fingerprint, Date revision) {

    }

    @Override
    public MultiMap<BareJid, OpenPgpV4Fingerprint> getAllContactsTrustedFingerprints() {
        return null;
    }

    @Override
    public byte[] getPublicKeyBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpPublicKeyException {
        return new byte[0];
    }

    @Override
    public byte[] getSecretKeyBytes(BareJid owner, OpenPgpV4Fingerprint fingerprint)
            throws MissingOpenPgpKeyPairException {
        return new byte[0];
    }
}
