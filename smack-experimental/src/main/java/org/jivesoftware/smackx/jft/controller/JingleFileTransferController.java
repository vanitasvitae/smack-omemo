package org.jivesoftware.smackx.jft.controller;

import org.jivesoftware.smackx.jft.listener.ProgressListener;
import org.jivesoftware.smackx.jingle.JingleDescriptionController;

/**
 * Created by vanitas on 27.07.17.
 */
public interface JingleFileTransferController extends JingleDescriptionController {

    void addProgressListener(ProgressListener listener);

    void removeProgressListener(ProgressListener listener);
}
