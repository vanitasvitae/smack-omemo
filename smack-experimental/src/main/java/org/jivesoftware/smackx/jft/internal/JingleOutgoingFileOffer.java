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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jft.internal.file.LocalFile;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleOutgoingFileOffer extends AbstractJingleFileOffer<LocalFile> implements OutgoingFileOfferController {
    private static final Logger LOGGER = Logger.getLogger(JingleOutgoingFileOffer.class.getName());

    public JingleOutgoingFileOffer(File file) {
        super(new LocalFile(file));
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        File mFile = ((LocalFile) file).getFile();
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            outputStream = bytestreamSession.getOutputStream();
            inputStream = new FileInputStream(mFile);

            byte[] fileBuf = new byte[(int) mFile.length()];

            inputStream.read(fileBuf);

            outputStream.write(fileBuf);
            outputStream.flush();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception while sending file: " + e, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close FileInputStream: " + e, e);
                }
            }
        }

        notifyProgressListenersFinished();
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
