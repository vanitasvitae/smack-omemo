package org.jivesoftware.smacks.messenger.android.csi.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Abstract class providing empty implementations of {@link Application.ActivityLifecycleCallbacks}.
 */
public abstract class AbstractActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}