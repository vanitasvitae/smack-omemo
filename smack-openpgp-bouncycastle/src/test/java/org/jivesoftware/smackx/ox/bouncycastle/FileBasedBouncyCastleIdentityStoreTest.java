/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.bouncycastle;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.util.XmppDateTime;

public class FileBasedBouncyCastleIdentityStoreTest extends SmackTestSuite {

    public static File storePath;

    @BeforeClass
    public static void setup() {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            storePath = new File(f, ".config/smack-integration-test/ox_store");
        } else {
            storePath = new File("int_test_ox_store");
        }
    }

    @Test
    public void writeReadPublicKeysLists() throws ParseException, IOException {
        BareJid jid = JidCreate.bareFrom("edward@snowden.org");
        String fp1 = "1357B01865B2503C18453D208CAC2A9678548E35";
        String fp2 = "67819B343B2AB70DED9320872C6464AF2A8E4C02";
        Date d1 = XmppDateTime.parseDate("2018-03-01T15:26:12Z");
        Date d2 = XmppDateTime.parseDate("1953-05-16T12:00:00Z");

        PublicKeysListElement list = PublicKeysListElement.builder()
                .addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fp1, d1))
                .addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fp2, d2))
                .build();

        FileBasedBouncyCastleIdentityStore store = new FileBasedBouncyCastleIdentityStore(storePath);

        PublicKeysListElement shouldBeNull = store.loadPubkeyList(jid);
        assertNull(shouldBeNull);
        store.storePubkeyList(jid, list);

        PublicKeysListElement retrieved = store.loadPubkeyList(jid);
        assertEquals(list.getMetadata(), retrieved.getMetadata());
    }

    @Before
    public void before() {
        deleteStore();
    }

    @After
    public void after() {
        deleteStore();
    }

    public void deleteStore() {
        FileUtils.deleteDirectory(storePath);
    }
}
