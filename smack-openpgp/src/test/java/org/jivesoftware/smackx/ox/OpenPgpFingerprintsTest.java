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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.chat.OpenPgpFingerprints;

import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class OpenPgpFingerprintsTest extends SmackTestSuite {

    private static final OpenPgpV4Fingerprint fingerprint1 = new OpenPgpV4Fingerprint("47696d6d6520323020636861726163746572730a");
    private static final OpenPgpV4Fingerprint fingerprint2 = new OpenPgpV4Fingerprint("492e206e6565642e20656173746572656767730a");
    private static final OpenPgpV4Fingerprint fingerprint3 = new OpenPgpV4Fingerprint("4d6f7265206f66207468656d21204d6f7265210a");

    @Test
    public void activeKeysTest() {
        Set<OpenPgpV4Fingerprint> announced = new HashSet<>();
        announced.add(fingerprint1);
        announced.add(fingerprint2);

        Set<OpenPgpV4Fingerprint> available = new HashSet<>();
        available.add(fingerprint2);
        available.add(fingerprint3);

        OpenPgpFingerprints fingerprints = new OpenPgpFingerprints(
                JidTestUtil.BARE_JID_1,
                announced,
                available,
                new HashMap<OpenPgpV4Fingerprint, Throwable>());

        assertEquals(announced, fingerprints.getAnnouncedKeys());
        assertEquals(available, fingerprints.getAvailableKeys());
        assertEquals(JidTestUtil.BARE_JID_1, fingerprints.getJid());
        assertEquals(0, fingerprints.getUnfetchableKeys().size());

        Set<OpenPgpV4Fingerprint> active = new HashSet<>();
        active.add(fingerprint2);

        assertEquals(active, fingerprints.getActiveKeys());
    }
}
