package org.jivesoftware.smackx.messenger.csi;

public interface ClientStateListener {
    void onClientInForeground();

    void onClientInBackground();
}
