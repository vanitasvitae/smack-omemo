package org.jivesoftware.smackx.jingle_filetransfer;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_filetransfer.element.Checksum;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;
import org.jivesoftware.smackx.jingle_filetransfer.provider.ChecksumProvider;

import org.junit.Test;

/**
 * Created by vanitas on 12.07.17.
 */
public class ChecksumTest extends SmackTestSuite {

    @Test
    public void parserTest() throws Exception {
        HashElement hash = new HashElement(HashManager.ALGORITHM.SHA_256, "f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=");
        JingleFileTransferChild file = new JingleFileTransferChild(null, null, hash, null, null, -1, null);
        Checksum checksum = new Checksum(JingleContent.Creator.initiator, "name", file);

        String xml = "<checksum xmlns='urn:xmpp:jingle:apps:file-transfer:5' creator='initiator' name='name'>" +
                file.toXML().toString() +
                "</checksum>";

        assertXMLEqual(xml, checksum.toXML().toString());
        assertXMLEqual(xml, new ChecksumProvider().parse(TestUtils.getParser(xml)).toXML().toString());
    }
}
