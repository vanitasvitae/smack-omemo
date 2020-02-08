package org.jivesoftware.smacks.messenger.android;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smacks.messenger.android.csi.util.AbstractActivityLifecycleCallbacks;
import org.jivesoftware.smackx.messenger.csi.ClientStateListener;

import android.app.Activity;
import android.app.Application;

/**
 * Android Utility class that observes the current state of the application.
 * If the application currently displays at least one active Activity, then registered
 * {@link ClientStateListener ClientStateListeners} will be notified via {@link ClientStateListener#onClientInForeground()}.
 * If the application goes into the background, {@link ClientStateListener#onClientInBackground()} will be fired.
 *
 * Setup: During application startup, call {@link android.app.Application#registerActivityLifecycleCallbacks(Application.ActivityLifecycleCallbacks)}
 * and pass an instance of {@link AndroidCsiManager} as argument.
 *
 * Remember to also register a {@link ClientStateListener} implementation (eg. the Messenger class from smack-messenger).
 */
public final class AndroidCsiManager extends AbstractActivityLifecycleCallbacks {

    private static AndroidCsiManager INSTANCE;

    private AtomicInteger activityReferences = new AtomicInteger(0);
    private AtomicBoolean isActivityChangingConfiguration = new AtomicBoolean(false);

    private final List<ClientStateListener> listeners = new ArrayList<>();

    private AndroidCsiManager() {

    }

    public AndroidCsiManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AndroidCsiManager();
        }
        return INSTANCE;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (activityReferences.incrementAndGet() == 1 && !isActivityChangingConfiguration.get()) {
            for (ClientStateListener listener : listeners) {
                listener.onClientInForeground();
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfiguration.set(activity.isChangingConfigurations());
        if (activityReferences.decrementAndGet() == 0 && !isActivityChangingConfiguration.get()) {
            for (ClientStateListener listener : listeners) {
                listener.onClientInBackground();
            }
        }
    }

    public void addClientStateListener(ClientStateListener listener) {
        this.listeners.add(listener);
    }

    public void removeClientStateListener(ClientStateListener listener) {
        this.listeners.remove(listener);
    }
}
