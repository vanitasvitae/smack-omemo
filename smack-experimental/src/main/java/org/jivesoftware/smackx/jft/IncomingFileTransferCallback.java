package org.jivesoftware.smackx.jft;

import java.io.File;

import org.jivesoftware.smackx.jft.internal.JingleIncomingFileOffer;

/**
 * Created by vanitas on 26.07.17.
 */
public class IncomingFileTransferCallback {

    private JingleIncomingFileOffer offer;

    public IncomingFileTransferCallback(JingleIncomingFileOffer offer) {
        this.offer = offer;
    }

    public JingleIncomingFileOffer accept(File target) {
        offer.accept(target);
        return offer;
    }

    public void decline() {
        offer.decline();
    }
}
