package org.jivesoftware.smackx.jft.internal;

import java.io.File;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleOutgoingFileOffer extends JingleFileOffer<LocalFile> {

    public JingleOutgoingFileOffer(File file) {
        super(new LocalFile(file));
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        
    }
}
