package org.jivesoftware.smackx.hash;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.hash.element.HashElement;
import org.jivesoftware.smackx.hash.provider.HashElementProvider;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by vanitas on 30.05.17.
 */
public class HashElementTest extends SmackTestSuite {

    @Test
    public void stanzaTest() throws Exception {
        String message = "Hello World!";
        HashElement element = HashElement.fromData(HashUtil.ALGORITHM.SHA_256, message.getBytes(StringUtils.UTF8));
        String expected = "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>";
        assertEquals(expected, element.toXML().toString());

        HashElement parsed = new HashElementProvider().parse(TestUtils.getParser(expected));
        assertEquals(expected, parsed.toXML().toString());
    }

}
