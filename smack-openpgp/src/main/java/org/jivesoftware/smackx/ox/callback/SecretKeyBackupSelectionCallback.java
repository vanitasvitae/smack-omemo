package org.jivesoftware.smackx.ox.callback;

import java.util.Set;

import org.jivesoftware.smackx.ox.OpenPgpV4Fingerprint;

/**
 * Callback to allow the user to decide, which locally available secret keys they want to include in a backup.
 */
public interface SecretKeyBackupSelectionCallback {

    /**
     * Let the user decide, which secret keys they want to backup.
     *
     * @param availableSecretKeys {@link Set} of {@link OpenPgpV4Fingerprint}s of locally available
     *                                       OpenPGP secret keys.
     * @return {@link Set} which contains the {@link OpenPgpV4Fingerprint}s the user decided to include
     *                                       in the backup.
     */
    Set<OpenPgpV4Fingerprint> selectKeysToBackup(Set<OpenPgpV4Fingerprint> availableSecretKeys);
}
