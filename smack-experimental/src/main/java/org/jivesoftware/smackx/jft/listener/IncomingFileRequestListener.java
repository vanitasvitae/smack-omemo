package org.jivesoftware.smackx.jft.listener;

import org.jivesoftware.smackx.jft.controller.IncomingFileRequestController;

/**
 * Created by vanitas on 27.07.17.
 */
public interface IncomingFileRequestListener {

    void onIncomingFileRequest(IncomingFileRequestController request);
}
