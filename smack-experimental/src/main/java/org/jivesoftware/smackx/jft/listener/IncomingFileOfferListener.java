package org.jivesoftware.smackx.jft.listener;

import org.jivesoftware.smackx.jft.controller.IncomingFileOfferController;

/**
 * Created by vanitas on 26.07.17.
 */
public interface IncomingFileOfferListener {

    void onIncomingFileTransfer(IncomingFileOfferController offer);
}
