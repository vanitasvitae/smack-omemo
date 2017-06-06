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
package org.jivesoftware.smackx.jingle_ibb;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleContentTransportManager;
import org.jivesoftware.smackx.jingle.JingleInputStream;
import org.jivesoftware.smackx.jingle.JingleTransportInputStreamCallback;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_ibb.element.JingleInBandBytestreamTransport;
import org.jivesoftware.smackx.jingle_ibb.provider.JingleInBandByteStreamTransportProvider;

/**
 * Manager for Jingle In-Band-Bytestreams.
 */
public final class JingleInBandBytestreamTransportManager extends Manager implements JingleContentTransportManager {

    private static final Logger LOGGER = Logger.getLogger(JingleInBandBytestreamTransportManager.class.getName());
    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:ibb:1";

    private static final WeakHashMap<XMPPConnection, JingleInBandBytestreamTransportManager> INSTANCES = new WeakHashMap<>();

    private JingleInBandBytestreamTransportManager(XMPPConnection connection) {
        super(connection);
        JingleContentProviderManager.addJingleContentTransportProvider(NAMESPACE_V1, new JingleInBandByteStreamTransportProvider());
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(NAMESPACE_V1);
    }

    public static JingleInBandBytestreamTransportManager getInstanceFor(XMPPConnection connection) {
        JingleInBandBytestreamTransportManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleInBandBytestreamTransportManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    @Override
    public void acceptInputStream(final Jingle jingle, final JingleTransportInputStreamCallback callback) {
        final int blockSize = ((JingleInBandBytestreamTransport)
                jingle.getContents().get(0).getJingleTransports().get(0)).getBlockSize();
        InBandBytestreamListener bytestreamListener = new InBandBytestreamListener() {
            @Override
            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                if (request.getSessionID().equals(jingle.getSid())) {
                    try {
                        InBandBytestreamSession ibs = request.accept();
                        InputStream inputStream = ibs.getInputStream();
                        callback.onInputStream(new JingleInputStream(inputStream, blockSize));
                    } catch (SmackException.NotConnectedException | InterruptedException e) {
                        LOGGER.log(Level.SEVERE, "Could not accept IBB session: " + e, e);
                    }
                }
            }
        };

        InBandBytestreamManager.getByteStreamManager(connection())
                .addIncomingBytestreamListener(bytestreamListener);
    }

    @Override
    public OutputStream createOutputStream(Jingle jingle) {
        JingleInBandBytestreamTransport transport = null;
        JingleContent content = jingle.getContents().get(0);
        for (JingleContentTransport t : content.getJingleTransports()) {
            if (t.getNamespace().equals(NAMESPACE_V1)) {
                transport = (JingleInBandBytestreamTransport) t;
            }
        }

        if (transport == null) {
            //TODO: Transport-failed
            return null;
        }

        InBandBytestreamManager ibm = InBandBytestreamManager.getByteStreamManager(connection());
        ibm.setMaximumBlockSize(transport.getBlockSize());
        InBandBytestreamSession ibs;
        try {
            ibs = ibm.establishSession(jingle.getFrom(), jingle.getSid());
        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            LOGGER.log(Level.SEVERE, "Fail in handle request: " + e, e);
            return null;
        }

        return ibs.getOutputStream();
    }

    /**
     * Generate a random session id.
     * @return
     */
    public static String generateSessionId() {
        return StringUtils.randomString(24);
    }
}
