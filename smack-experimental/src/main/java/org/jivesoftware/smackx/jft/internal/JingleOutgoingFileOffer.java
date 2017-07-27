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

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }
}
