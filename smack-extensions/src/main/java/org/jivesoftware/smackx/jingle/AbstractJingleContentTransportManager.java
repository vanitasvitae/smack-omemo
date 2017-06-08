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
package org.jivesoftware.smackx.jingle;

import java.io.OutputStream;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.provider.JingleContentTransportProvider;
import org.jxmpp.jid.Jid;

/**
 * Interface with methods that JingleContentTransportManagers must implement.
 */
public abstract class AbstractJingleContentTransportManager<D extends JingleContentTransport> extends Manager {

    public AbstractJingleContentTransportManager(XMPPConnection connection) {
        super(connection);
        JingleTransportManager.getInstanceFor(connection).registerJingleContentTransportManager(this);
        JingleContentProviderManager.addJingleContentTransportProvider(getNamespace(), createJingleContentTransportProvider());
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());
    }

    protected abstract JingleContentTransportProvider<D> createJingleContentTransportProvider();

    public abstract String getNamespace();

    public abstract void acceptInputStream(Jingle jingle, JingleTransportInputStreamCallback callback);

    public abstract OutputStream createOutputStream(Jingle jingle);

    public abstract D createJingleContentTransport(Jid remote) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException;
}
