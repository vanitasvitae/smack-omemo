package org.jivesoftware.smackx.jingle;

import static junit.framework.TestCase.fail;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.Assert;

/**
 * Created by vanitas on 27.07.17.
 */
public class JingleIBBTest extends AbstractSmackIntegrationTest {

    public JingleIBBTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void testIBBTransport() throws Exception {
        JingleIBBTransport sender = new JingleIBBTransport();
        JingleIBBTransport receiver = new JingleIBBTransport();

        final SimpleResultSyncPoint recvPoint = new SimpleResultSyncPoint();

        final byte[] data = new byte[512];
        new Random().nextBytes(data);
        final byte[] recv = new byte[512];

        receiver.establishIncomingBytestreamSession(conOne, new JingleTransportCallback() {
            @Override
            public void onTransportReady(BytestreamSession bytestreamSession) {
                try {
                    bytestreamSession.getInputStream().read(recv);
                    bytestreamSession.getInputStream().close();
                    recvPoint.signal();
                } catch (IOException e) {
                    fail(e.toString());
                }
            }

            @Override
            public void onTransportFailed(Exception e) {
                LOGGER.log(Level.SEVERE, e.toString());
                recvPoint.signal();
            }
        });

        sender.establishOutgoingBytestreamSession(conTwo, new JingleTransportCallback() {
            @Override
            public void onTransportReady(BytestreamSession bytestreamSession) {
                try {
                    bytestreamSession.getOutputStream().write(data);
                } catch (IOException e) {
                    fail(e.toString());
                }
            }

            @Override
            public void onTransportFailed(Exception e) {

            }
        });

        recvPoint.wait(10 * 1000);
        Assert.assertArrayEquals(data, recv);
    }
}
