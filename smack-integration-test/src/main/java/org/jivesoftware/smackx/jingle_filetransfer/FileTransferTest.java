package org.jivesoftware.smackx.jingle_filetransfer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_filetransfer.callback.IncomingFileOfferCallback;
import org.jivesoftware.smackx.jingle_filetransfer.listener.JingleFileTransferOfferListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.junit.AfterClass;
import org.jxmpp.jid.FullJid;

/**
 * Created by vanitas on 29.06.17.
 */
public class FileTransferTest extends AbstractSmackIntegrationTest {

    private static final File tempDir;

    static {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            tempDir = new File(f, ".config/smack-integration-test/");
        } else {
            tempDir = new File("int_test_jingle");
        }
    }

    public FileTransferTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void basicFileTransferTest() {

        FullJid alice = conOne.getUser().asFullJidOrThrow();
        FullJid bob = conTwo.getUser().asFullJidOrThrow();

        File source = prepareNewTestFile("source");
        final File target = new File(tempDir, "target");

        JingleFileTransferManager aftm = JingleFileTransferManager.getInstanceFor(conOne);
        JingleFileTransferManager bftm = JingleFileTransferManager.getInstanceFor(conTwo);

        bftm.addJingleFileTransferOfferListener(new JingleFileTransferOfferListener() {
            @Override
            public void onFileOffer(Jingle request, IncomingFileOfferCallback callback) {
                callback.acceptIncomingFileOffer(request, target);
            }
        });

        try {
            aftm.sendFile(bob, source);
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
            fail();
        }

        byte[] sBytes = new byte[(int) source.length()];
        byte[] tBytes = new byte[(int) target.length()];
        try {
            FileInputStream fi = new FileInputStream(source);
            fi.read(sBytes);
            fi.close();
            fi = new FileInputStream(target);
            fi.read(tBytes);
            fi.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read files.");
            fail();
        }

        assertArrayEquals(sBytes, tBytes);
        LOGGER.log(Level.INFO, "SUCCESSFULLY SENT AND RECEIVED");

    }

    private File prepareNewTestFile(String name) {
        File testFile = new File(tempDir, name);
        try {
            if (!testFile.exists()) {
                testFile.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(testFile);
            byte[] rand = new byte[16000];
            INSECURE_RANDOM.nextBytes(rand);
            fo.write(rand);
            fo.close();
            return testFile;
        } catch (IOException e) {
            return null;
        }
    }

    @AfterClass
    public void cleanup() {
        Socks5Proxy.getSocks5Proxy().stop();
    }
}
