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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoMessageInformation;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class OmemoMessageInformationTest {

    @Test
    public void setterGetterTest() throws XmppStringprepException {
        OmemoMessageInformation information = new OmemoMessageInformation();
        assertEquals(information.getCarbon(), OmemoMessageInformation.CARBON.NONE);
        information.setCarbon(OmemoMessageInformation.CARBON.RECV);
        assertEquals(OmemoMessageInformation.CARBON.RECV, information.getCarbon());

        assertNull(information.getSenderDevice());
        OmemoDevice device = new OmemoDevice(JidCreate.bareFrom("test@a.bc"), 14);
        information.setSenderDevice(device);
        assertEquals(device, information.getSenderDevice());
    }
}
