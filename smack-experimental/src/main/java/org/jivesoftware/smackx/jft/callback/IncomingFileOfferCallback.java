/**
 *
 * Copyright 2017 Paul Schaub
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
