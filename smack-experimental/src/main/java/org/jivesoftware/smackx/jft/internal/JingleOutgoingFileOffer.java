package org.jivesoftware.smackx.jft.internal;

import java.io.File;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.internal.file.LocalFile;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleOutgoingFileOffer extends AbstractJingleFileOffer<LocalFile> implements OutgoingFileOfferController {

    public JingleOutgoingFileOffer(File file) {
        super(new LocalFile(file));
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        
    }
}
