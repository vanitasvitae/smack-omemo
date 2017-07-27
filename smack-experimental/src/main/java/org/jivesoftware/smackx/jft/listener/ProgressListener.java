package org.jivesoftware.smackx.jft.listener;

/**
 * Created by vanitas on 27.07.17.
 */
public interface ProgressListener {

    void started();

    void progress(float percent);

    void finished();
}
