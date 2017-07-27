package org.jivesoftware.smackx.jft.callback;

import java.io.File;

import org.jivesoftware.smackx.jft.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle.callbacks.JingleCallback;

/**
 * Created by vanitas on 27.07.17.
 */
public interface IncomingFileOfferCallback extends JingleCallback<IncomingFileOfferCallback.Destination> {

    @Override
    IncomingFileOfferController accept(Destination destination);

    class Destination extends JingleCallback.Parameters {
        private final File destination;

        public Destination(File destination) {
            this.destination = destination;
        }

        public File getDestination() {
            return destination;
        }
    }
}
