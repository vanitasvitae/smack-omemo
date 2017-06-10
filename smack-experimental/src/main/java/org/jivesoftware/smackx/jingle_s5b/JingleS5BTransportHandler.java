package org.jivesoftware.smackx.jingle_s5b;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSessionHandler;
import org.jivesoftware.smackx.jingle.JingleTransportEstablishedCallback;
import org.jivesoftware.smackx.jingle.JingleTransportHandler;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle_s5b.elements.JingleS5BTransportCandidate;

/**
 * JingleTransportHandler for Socks5Bytestreams.
 */
public class JingleS5BTransportHandler implements JingleTransportHandler<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportHandler.class.getName());

    private final WeakReference<JingleSessionHandler> sessionHandler;

    public JingleS5BTransportHandler(JingleSessionHandler sessionHandler) {
        this.sessionHandler = new WeakReference<>(sessionHandler);
    }

    @Override
    public void establishOutgoingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport hopefullyS5BTransport, JingleTransportEstablishedCallback callback) {
        if (!hopefullyS5BTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Transport must be a JingleS5BTransport.");
        }
        JingleS5BTransport transport = (JingleS5BTransport) hopefullyS5BTransport;



        ArrayList<Bytestream.StreamHost> streamHosts = new ArrayList<>();
        for (JingleContentTransportCandidate c : transport.getCandidates()) {
            streamHosts.add(((JingleS5BTransportCandidate) c).getStreamHost());
        }

        for (Bytestream.StreamHost streamHost : streamHosts) {
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            LOGGER.log(Level.INFO, "Connect outwards to " + address);
            // establish socket
            try {

                // build SOCKS5 client
                final Socks5Client socks5Client = new Socks5Client(streamHost, transport.getDestinationAddress());

                // connect to SOCKS5 proxy with a timeout
                Socket socket = socks5Client.getSocket(10 * 1000);

                // set selected host
                break;

            }
            catch (TimeoutException | IOException | SmackException | XMPPException e) {

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void establishIncomingSession(JingleManager.FullJidAndSessionId target, JingleContentTransport hopefullyS5BTransport, JingleTransportEstablishedCallback callback) {
        if (!hopefullyS5BTransport.getNamespace().equals(JingleS5BTransport.NAMESPACE_V1)) {
            throw new IllegalArgumentException("Transport must be a JingleS5BTransport.");
        }
        JingleS5BTransport transport = (JingleS5BTransport) hopefullyS5BTransport;



        ArrayList<Bytestream.StreamHost> streamHosts = new ArrayList<>();
        for (JingleContentTransportCandidate c : transport.getCandidates()) { //TODO Sort
            streamHosts.add(((JingleS5BTransportCandidate) c).getStreamHost());
        }

        for (Bytestream.StreamHost streamHost : streamHosts) {
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            LOGGER.log(Level.INFO, "Connect inwards to " + address);
            // establish socket
            try {

                // build SOCKS5 client
                final Socks5Client socks5Client = new Socks5Client(streamHost, transport.getDestinationAddress());

                // connect to SOCKS5 proxy with a timeout
                Socket socket = socks5Client.getSocket(10 * 1000);

                // set selected host
                break;

            }
            catch (TimeoutException | IOException | SmackException | XMPPException e) {

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public XMPPConnection getConnection() {
        JingleSessionHandler handler = sessionHandler.get();
        return handler != null ? handler.getConnection() : null;
    }
}
