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
package org.jivesoftware.smackx.omemo;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.util.HashSet;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class OmemoServiceTest extends SmackTestSuite {

    @Test(expected = IllegalStateException.class)
    public void getInstanceFailsWhenNullTest() {
        OmemoService.getInstance();
    }

    @Test
    public void isServiceRegisteredTest() {
        assertFalse(OmemoService.isServiceRegistered());
    }

    @Test
    public void removeOurDeviceTest() throws XmppStringprepException {
        OmemoDevice a = new OmemoDevice(JidCreate.bareFrom("a@b.c"), 123);
        OmemoDevice b = new OmemoDevice(JidCreate.bareFrom("a@b.c"), 124);

        HashSet<OmemoDevice> devices = new HashSet<>();
        devices.add(a); devices.add(b);

        assertTrue(devices.contains(a));
        assertTrue(devices.contains(b));

        devices.remove(a);
        assertFalse(devices.contains(a));
        assertTrue(devices.contains(b));
    }
}
