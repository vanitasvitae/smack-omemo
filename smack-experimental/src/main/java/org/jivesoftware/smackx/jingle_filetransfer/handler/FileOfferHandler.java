package org.jivesoftware.smackx.jingle_filetransfer.handler;

import org.jivesoftware.smackx.jingle.element.Jingle;

/**
 * Created by vanitas on 02.06.17.
 */
public interface FileOfferHandler {
    void handleFileOffer(Jingle jingle);
}
