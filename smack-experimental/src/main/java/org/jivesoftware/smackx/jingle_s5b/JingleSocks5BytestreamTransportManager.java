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
package org.jivesoftware.smackx.jingle_s5b;

import java.io.OutputStream;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.AbstractJingleContentTransportManager;
import org.jivesoftware.smackx.jingle.JingleTransportInputStreamCallback;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleSocks5BytestreamTransport;
import org.jivesoftware.smackx.jingle_s5b.provider.JingleSocks5BytestreamTransportProvider;

/**
 * Manager for JingleSocks5BytestreamTransports.
 */
public final class JingleSocks5BytestreamTransportManager extends AbstractJingleContentTransportManager<JingleSocks5BytestreamTransport> {

    private JingleSocks5BytestreamTransportManager(XMPPConnection connection) {
        super(connection);
    }

    @Override
    protected JingleContentTransportProvider<JingleSocks5BytestreamTransport> createJingleContentTransportProvider() {
        return new JingleSocks5BytestreamTransportProvider();
    }

    @Override
    public String getNamespace() {
        return JingleSocks5BytestreamTransport.NAMESPACE_V1;
    }

    @Override
    public void acceptInputStream(Jingle jingle, JingleTransportInputStreamCallback callback) {

    }

    @Override
    public OutputStream createOutputStream(Jingle jingle) {
        return null;
    }
}
