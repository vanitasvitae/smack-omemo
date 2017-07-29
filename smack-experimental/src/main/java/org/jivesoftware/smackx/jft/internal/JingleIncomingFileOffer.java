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
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jft.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jft.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jft.internal.file.RemoteFile;
import org.jivesoftware.smackx.jingle.components.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Created by vanitas on 26.07.17.
 */
public class JingleIncomingFileOffer extends AbstractJingleFileOffer<RemoteFile> implements IncomingFileOfferController {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());

    public JingleIncomingFileOffer(JingleFileTransferChildElement offer) {
        super(new RemoteFile(offer));
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public void onTransportReady(BytestreamSession bytestreamSession) {
        InputStream inputStream;
        try {
            inputStream = bytestreamSession.getInputStream();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get InputStream from BytestreamSession: " + e, e);
            return;
        }
    }

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    @Override
    public Future<Void> accept(XMPPConnection connection, File target)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {
        JingleSession session = getParent().getParent();
        if (session.getSessionState() == JingleSession.SessionState.pending) {
            session.accept(connection);
        }

        return null;
    }
}
